package com.myagree.app.common;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The MongoDB connection. Spring Boot builds the client from {@code spring.mongodb.uri} (server, database, user and
 * password); this adds the settings a URI rarely carries, from {@code agriscan.mongodb.*}. The client connects on
 * first use, so the app starts even while MongoDB is down.
 */
@Configuration(proxyBeanMethods = false)
public class MongoConfig {

    /** How the app shows up in MongoDB's logs and in {@code db.currentOp()}. */
    static final String APPLICATION_NAME = "agriscan-backend";

    /**
     * Fails fast when MongoDB is unreachable, instead of after the driver's 30 seconds, and bounds the connection
     * pool.
     */
    @Bean
    MongoClientSettingsBuilderCustomizer agriScanMongoClientSettings(AgriScanProperties properties) {
        AgriScanProperties.Mongodb mongodb = properties.mongodb();
        return settings -> settings
                .applicationName(APPLICATION_NAME)
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(mongodb.connectTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .applyToClusterSettings(cluster -> cluster
                        .serverSelectionTimeout(mongodb.serverSelectionTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .applyToConnectionPoolSettings(pool -> pool.maxSize(mongodb.maxPoolSize()));
    }
}
