package com.myagree.app.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.myagree.app.common.spi.PaymentPurpose;

/**
 * The activity log against a real MongoDB: the local "MongoDB" service on 127.0.0.1:27017, in a database of its own
 * that is dropped after every test. Skipped on machines where no MongoDB listens there.
 */
@EnabledIf("mongoIsRunning")
class PaymentActivityMongoTest {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 27017;
    private static final String DATABASE = "agriscan_test_" + ProcessHandle.current().pid();
    private static final Instant AT = Instant.parse("2026-09-29T04:30:00Z");

    private static MongoClient client;
    private MongoTemplate mongo;

    static boolean mongoIsRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void connect() {
        client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://" + HOST + ":" + PORT))
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(3, TimeUnit.SECONDS))
                .build());
    }

    @AfterAll
    static void disconnect() {
        client.close();
    }

    @BeforeEach
    void emptyDatabase() {
        mongo = new MongoTemplate(client, DATABASE);
        mongo.getDb().drop();
    }

    @AfterEach
    void dropDatabase() {
        mongo.getDb().drop();
    }

    @Test
    void theCollectionIsCreatedWithTheSchemaAsAStrictValidatorAndWithItsIndexes() {
        new PaymentActivitySchema(mongo).apply();

        Document options = collectionOptions();
        assertThat(options.get("validator", Document.class)).containsKey("$jsonSchema");
        assertThat(options.getString("validationLevel")).isEqualTo("strict");
        assertThat(options.getString("validationAction")).isEqualTo("error");
        assertThat(mongo.indexOps(PaymentActivity.COLLECTION).getIndexInfo())
                .extracting(IndexInfo::getName)
                .contains(PaymentActivitySchema.PAYMENT_TIMELINE_INDEX, PaymentActivitySchema.FARMER_ACTIVITY_INDEX);
    }

    @Test
    void anExistingCollectionIsBroughtUpToTheCurrentSchema() {
        mongo.createCollection(PaymentActivity.COLLECTION);

        new PaymentActivitySchema(mongo).apply();
        new PaymentActivitySchema(mongo).apply();

        assertThat(collectionOptions().get("validator", Document.class)).containsKey("$jsonSchema");
    }

    @Test
    void theLogStoresAChangeAndMongoDbRejectsDocumentsOutsideTheSchema() {
        PaymentActivity failed = new PaymentActivity(7, PaymentActivity.Type.FAILED, PaymentActivity.Source.CONFIRMATION,
                PaymentPurpose.STORE_ORDER, 12, 3, 730, PaymentProviderKind.SIMULATED, "sim_7",
                "Declined in the test checkout", AT);

        new PaymentActivityLog(mongo, new PaymentActivitySchema(mongo)).write(failed);

        Document stored = mongo.getCollection(PaymentActivity.COLLECTION).find().first();
        assertThat(stored)
                .containsEntry("paymentId", 7L)
                .containsEntry("type", "FAILED")
                .containsEntry("source", "CONFIRMATION")
                .containsEntry("failureReason", "Declined in the test checkout")
                .containsEntry("occurredAt", Date.from(AT));
        assertThatThrownBy(() -> mongo.insert(new Document("paymentId", 7L).append("type", "PAID"),
                PaymentActivity.COLLECTION))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Document failed validation");
    }

    private Document collectionOptions() {
        Document collection = mongo.getDb().listCollections()
                .filter(new Document("name", PaymentActivity.COLLECTION))
                .first();
        assertThat(collection).isNotNull();
        return collection.get("options", Document.class);
    }
}
