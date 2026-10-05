package com.myagree.app.notification;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import com.myagree.app.common.i18n.Language;

/**
 * The open notification streams ({@code GET /api/notifications/stream}) of every signed-in user, and the pushes to
 * them (docs/architecture/phase-2.md, D10). A user may keep several open, one per browser tab, and each stream shows
 * notifications in the language it was opened in. A stream sends a {@value #PING_EVENT} event as it opens and every
 * {@value #HEARTBEAT_SECONDS} seconds, so the client and any proxy in between see a live connection, and it ends after
 * {@link #STREAM_LIFETIME}, when the client reconnects with its current access token.
 *
 * <p>Resources are bounded: opening more than {@value #MAX_STREAMS_PER_USER} streams closes the user's oldest, and no
 * stream opens while {@value #MAX_OPEN_STREAMS} are open. A stream that completes, times out or fails is forgotten at
 * once, and one whose client went away is forgotten on the next write to it. Streams live in this instance's memory;
 * running several instances would need a shared broker (e.g. Redis pub/sub) to fan pushes out.
 */
@Component
class NotificationStreams implements SmartLifecycle {

    static final String NOTIFICATION_EVENT = "notification";
    static final String PING_EVENT = "ping";
    static final long HEARTBEAT_SECONDS = 25;
    static final Duration STREAM_LIFETIME = Duration.ofMinutes(30);
    /** A few tabs; below the six connections a browser opens to one server, which the app's API calls share. */
    static final int MAX_STREAMS_PER_USER = 5;
    /** Far below the servlet container's connection limit (Tomcat: 8,192), so streams never crowd out API calls. */
    static final int MAX_OPEN_STREAMS = 1_000;

    private static final Logger log = LoggerFactory.getLogger(NotificationStreams.class);

    /** Each user's open streams, oldest first. The lists are immutable and replaced as a whole. */
    private final ConcurrentMap<Long, List<Subscriber>> subscribersByUser = new ConcurrentHashMap<>();
    private final AtomicInteger openStreams = new AtomicInteger();
    private final Clock clock;
    private volatile boolean running;

    NotificationStreams(Clock clock) {
        this.clock = clock;
    }

    /**
     * Opens a stream of the user's new notifications, shown in {@code language}. When the user already has
     * {@value #MAX_STREAMS_PER_USER} streams open, the oldest is closed; its client reconnects if it is still there.
     *
     * @throws StreamUnavailableException while {@value #MAX_OPEN_STREAMS} streams are open, or during shutdown
     */
    SseEmitter open(long userId, Language language) {
        reserveStream();
        Subscriber subscriber = new Subscriber(userId, language, newEmitter());
        SseEmitter emitter = subscriber.emitter();
        emitter.onCompletion(() -> forget(subscriber));
        emitter.onError(error -> forget(subscriber));
        // Completing ends the response normally; a timeout left alone would be answered as an error.
        emitter.onTimeout(() -> close(subscriber));
        List<Subscriber> streams = subscribersByUser.merge(userId, List.of(subscriber), NotificationStreams::concat);
        streams.subList(0, Math.max(0, streams.size() - MAX_STREAMS_PER_USER)).forEach(this::close);
        // Written as soon as Spring starts the response, so the client sees the stream open right away.
        send(subscriber, ping());
        return emitter;
    }

    /**
     * Sends a notification to each of the user's open streams, rendered once per language those streams use. A stream
     * that fails is forgotten, and the others still receive the notification.
     *
     * @param notification the notification's JSON body in a given language
     */
    void push(long userId, Function<Language, ?> notification) {
        Map<Language, Object> rendered = new EnumMap<>(Language.class);
        for (Subscriber subscriber : subscribersByUser.getOrDefault(userId, List.of())) {
            Object body = rendered.computeIfAbsent(subscriber.language(), notification);
            send(subscriber, SseEmitter.event().name(NOTIFICATION_EVENT).data(body, MediaType.APPLICATION_JSON));
        }
    }

    /** Pings every open stream, which also finds the streams whose clients went away. */
    @Scheduled(fixedRate = HEARTBEAT_SECONDS, timeUnit = TimeUnit.SECONDS)
    void heartbeat() {
        subscribersByUser.values().forEach(subscribers -> subscribers.forEach(subscriber -> send(subscriber, ping())));
    }

    @Override
    public void start() {
        running = true;
    }

    /**
     * Closes every stream as the application shuts down. Lifecycle beans stop from the highest phase down, and this
     * one keeps the highest, so it stops before the web server's graceful shutdown, which would otherwise wait for the
     * open streams to end.
     */
    @Override
    public void stop() {
        running = false;
        subscribersByUser.values().forEach(subscribers -> subscribers.forEach(this::close));
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** A new stream's emitter; the seam through which tests see what is sent. */
    SseEmitter newEmitter() {
        return new SseEmitter(STREAM_LIFETIME.toMillis());
    }

    private void reserveStream() {
        if (!running) {
            throw new StreamUnavailableException();
        }
        if (openStreams.incrementAndGet() > MAX_OPEN_STREAMS) {
            openStreams.decrementAndGet();
            log.warn("Refused a notification stream because {} streams are open", MAX_OPEN_STREAMS);
            throw new StreamUnavailableException();
        }
    }

    private void send(Subscriber subscriber, SseEventBuilder event) {
        try {
            subscriber.emitter().send(event);
        } catch (IOException | IllegalStateException streamGone) {
            // The client went away or the stream has just ended; a client that is still there reconnects by itself.
            log.debug("Forgetting a notification stream of user {}: {}", subscriber.userId(), streamGone.toString());
            forget(subscriber);
        }
    }

    /** Ends the stream's response and forgets the stream. */
    private void close(Subscriber subscriber) {
        forget(subscriber);
        subscriber.emitter().complete();
    }

    /** Stops sending to the stream. Safe to repeat: completion follows a timeout, an error and every close. */
    private void forget(Subscriber subscriber) {
        if (subscriber.markForgotten()) {
            subscribersByUser.computeIfPresent(subscriber.userId(), (userId, streams) -> without(streams, subscriber));
            openStreams.decrementAndGet();
        }
    }

    private SseEventBuilder ping() {
        return SseEmitter.event().name(PING_EVENT).data(clock.instant().toString());
    }

    private static List<Subscriber> concat(List<Subscriber> older, List<Subscriber> newer) {
        return Stream.concat(older.stream(), newer.stream()).toList();
    }

    /** The streams without {@code forgotten}, or {@code null}, which removes the user's entry, when none remain. */
    private static @Nullable List<Subscriber> without(List<Subscriber> streams, Subscriber forgotten) {
        List<Subscriber> remaining = streams.stream().filter(stream -> stream != forgotten).toList();
        return remaining.isEmpty() ? null : remaining;
    }

    /** One open stream: whose it is, the language it shows notifications in, and the response it writes to. */
    private static final class Subscriber {

        private final long userId;
        private final Language language;
        private final SseEmitter emitter;
        private final AtomicBoolean forgotten = new AtomicBoolean();

        Subscriber(long userId, Language language, SseEmitter emitter) {
            this.userId = userId;
            this.language = language;
            this.emitter = emitter;
        }

        long userId() {
            return userId;
        }

        Language language() {
            return language;
        }

        SseEmitter emitter() {
            return emitter;
        }

        /** Whether this call is the first to forget the stream, so it is counted out exactly once. */
        boolean markForgotten() {
            return forgotten.compareAndSet(false, true);
        }
    }
}
