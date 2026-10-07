package com.myagree.app.payment;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mongodb.MongoException;

/**
 * Keeps every change of every payment in MongoDB (collection {@value PaymentActivity#COLLECTION}), for support and
 * audits; the SQL database stays the source of truth. A change is written once its transaction has committed, so a
 * payment that rolled back never shows up, and on a background thread of its own, so a slow or stopped MongoDB never
 * delays or fails a payment: the change is then only logged as lost.
 */
@Component
@ConditionalOnBooleanProperty("agriscan.mongodb.enabled")
class PaymentActivityLog implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(PaymentActivityLog.class);

    /** Changes waiting to be written; while MongoDB is down for long, newer ones beyond this are dropped. */
    private static final int QUEUE_CAPACITY = 1_000;
    private static final long SHUTDOWN_GRACE_SECONDS = 10;

    private final MongoTemplate mongo;
    private final PaymentActivitySchema schema;
    // One thread writes the changes in the order they happened.
    private final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable, "payment-activity-log");
                thread.setDaemon(true);
                return thread;
            },
            (runnable, executor) -> log.warn("Payment activity queue is full; a change was not written to MongoDB"));

    PaymentActivityLog(MongoTemplate mongo, PaymentActivitySchema schema) {
        this.mongo = mongo;
        this.schema = schema;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(PaymentActivity activity) {
        writer.execute(() -> write(activity));
    }

    /** Writes one change now, setting the collection up first if MongoDB was unreachable until now. */
    void write(PaymentActivity activity) {
        try {
            schema.apply();
            mongo.insert(activity.toDocument(), PaymentActivity.COLLECTION);
        } catch (DataAccessException | MongoException e) {
            log.warn("Could not write {} of payment {} to MongoDB: {}", activity.type(), activity.paymentId(),
                    e.getMessage());
        }
    }

    /** Lets the changes still queued reach MongoDB before the app stops. */
    @Override
    public void destroy() throws InterruptedException {
        writer.shutdown();
        if (!writer.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
            log.warn("{} payment changes were not written to MongoDB before shutdown", writer.shutdownNow().size());
        }
    }
}
