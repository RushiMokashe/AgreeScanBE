package com.myagree.app.payment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.validation.Validator;
import org.springframework.stereotype.Component;

import com.mongodb.MongoException;

/**
 * Sets up collection {@value PaymentActivity#COLLECTION}: creates it with the JSON schema in {@value #SCHEMA} as a
 * strict validator, so MongoDB rejects documents that do not match, or brings the validator of an existing one up to
 * date; then makes sure its indexes exist. Every step is safe to repeat.
 */
@Component
@ConditionalOnBooleanProperty("agriscan.mongodb.enabled")
class PaymentActivitySchema {

    private static final Logger log = LoggerFactory.getLogger(PaymentActivitySchema.class);

    static final String SCHEMA = "mongodb/payment_events.schema.json";
    /** A payment's history in order: {@code find({paymentId: 7}).sort({occurredAt: 1})}. */
    static final String PAYMENT_TIMELINE_INDEX = "payment_timeline";
    /** A farmer's latest payment activity first. */
    static final String FARMER_ACTIVITY_INDEX = "farmer_activity";

    private final MongoTemplate mongo;
    private final Document validator;
    private volatile boolean applied;

    PaymentActivitySchema(MongoTemplate mongo) {
        this.mongo = mongo;
        this.validator = new Document("$jsonSchema", Document.parse(readSchema()));
    }

    /** Sets the collection up as soon as the app is ready, so a MongoDB that cannot be reached shows in the log. */
    @EventListener(ApplicationReadyEvent.class)
    void applyAtStartup() {
        try {
            apply();
            log.info("MongoDB is ready: collection {} in database {}", PaymentActivity.COLLECTION, mongo.getDb().getName());
        } catch (DataAccessException | MongoException e) {
            log.warn("MongoDB cannot be reached ({}); payment activity is logged from when it can", e.getMessage());
        }
    }

    /**
     * Creates or updates the collection, its validator and its indexes, once per run of the app.
     *
     * @throws DataAccessException when MongoDB cannot be reached or refuses; the next call tries again
     */
    synchronized void apply() {
        if (applied) {
            return;
        }
        if (mongo.collectionExists(PaymentActivity.COLLECTION)) {
            mongo.executeCommand(new Document("collMod", PaymentActivity.COLLECTION)
                    .append("validator", validator)
                    .append("validationLevel", "strict")
                    .append("validationAction", "error"));
        } else {
            mongo.createCollection(PaymentActivity.COLLECTION, CollectionOptions.empty()
                    .validator(Validator.document(validator))
                    .strictValidation()
                    .failOnValidationError());
        }
        IndexOperations indexes = mongo.indexOps(PaymentActivity.COLLECTION);
        indexes.createIndex(new Index()
                .on("paymentId", Sort.Direction.ASC)
                .on("occurredAt", Sort.Direction.ASC)
                .named(PAYMENT_TIMELINE_INDEX));
        indexes.createIndex(new Index()
                .on("farmerId", Sort.Direction.ASC)
                .on("occurredAt", Sort.Direction.DESC)
                .named(FARMER_ACTIVITY_INDEX));
        applied = true;
    }

    private static String readSchema() {
        try {
            return new ClassPathResource(SCHEMA).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the MongoDB schema " + SCHEMA, e);
        }
    }
}
