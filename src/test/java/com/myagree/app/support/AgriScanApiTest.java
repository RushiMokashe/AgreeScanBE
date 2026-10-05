package com.myagree.app.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full-stack API test against the seeded demo data with a fixed clock and a fixed signing secret. Every test runs
 * in a transaction that is rolled back afterwards, so tests never see each other's changes. All test classes share
 * one application context, unless one adds configuration of its own. Autowire {@link TestUsers} to call the API as a
 * signed-in user; {@link TestMediaSource} serves media kind "test".
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:agriscan-test;DB_CLOSE_DELAY=-1",
        "agriscan.uploads-dir=target/test-uploads",
        "agriscan.security.jwt-secret=agriscan-test-secret-not-for-production-use"})
@AutoConfigureMockMvc
@Import({FixedClockConfiguration.class, TestUsers.class, TestMediaSource.class})
@Transactional
public @interface AgriScanApiTest {
}
