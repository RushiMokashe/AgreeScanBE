package com.myagree.app.common.media;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.myagree.app.common.ForbiddenException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.UserMessage;

/** Resolves signed media URLs to files, through the {@link MediaSource} registered for each kind. */
@Service
class MediaService {

    private static final UserMessage LINK_INVALID = UserMessage.of("common.media.link-invalid");

    private final Map<String, MediaSource> sourcesByKind;
    private final MediaUrlSigner signer;
    private final Clock clock;

    MediaService(List<MediaSource> sources, MediaUrlSigner signer, Clock clock) {
        this.sourcesByKind = sources.stream().collect(Collectors.toUnmodifiableMap(MediaSource::kind, Function.identity(),
                (first, second) -> {
                    throw new IllegalStateException("Two media sources serve kind '%s'".formatted(first.kind()));
                }));
        this.signer = signer;
        this.clock = clock;
    }

    /**
     * The file behind a signed media URL.
     *
     * @throws ForbiddenException for a forged, altered or expired URL
     * @throws NotFoundException  when no source serves the kind, or the source has no such file
     */
    SignedMedia open(String kind, String id, long expiresAt, String signature) {
        if (!signer.isValid(kind, id, expiresAt, signature)) {
            throw new ForbiddenException(LINK_INVALID);
        }
        MediaContent content = Optional.ofNullable(sourcesByKind.get(kind))
                .flatMap(source -> source.load(id))
                .orElseThrow(() -> NotFoundException.of("Media", kind + "/" + id));
        return new SignedMedia(content, Duration.between(clock.instant(), Instant.ofEpochSecond(expiresAt)));
    }
}
