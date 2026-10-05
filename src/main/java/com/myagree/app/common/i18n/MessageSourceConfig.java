package com.myagree.app.common.i18n;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * The message bundles behind {@link Messages}, set up from {@code spring.messages.*} as Spring Boot would, except that
 * a namespace whose feature has no bundle yet is quietly skipped in every language. Spring itself fails the lookup for
 * locales the JVM does not list, which includes every {@link Language#locale()} ({@code mr-IN-u-nu-latn}), and logs a
 * warning per lookup for the others, such as the request locales Spring Security's messages are resolved in.
 */
@Configuration(proxyBeanMethods = false)
class MessageSourceConfig {

    @Bean
    @ConfigurationProperties("spring.messages")
    MessageSourceProperties messageSourceProperties() {
        return new MessageSourceProperties();
    }

    /** Replaces Spring Boot's auto-configured message source, which backs off for a bean of this name. */
    @Bean
    MessageSource messageSource(MessageSourceProperties properties) throws IOException {
        ResourceBundleMessageSource messageSource = new NamespaceBundleMessageSource();
        messageSource.setBasenames(properties.getBasename().toArray(String[]::new));
        if (properties.getEncoding() != null) {
            messageSource.setDefaultEncoding(properties.getEncoding().name());
        }
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        if (properties.getCacheDuration() != null) {
            messageSource.setCacheMillis(properties.getCacheDuration().toMillis());
        }
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
        messageSource.setCommonMessages(commonMessages(properties));
        return messageSource;
    }

    private static @Nullable Properties commonMessages(MessageSourceProperties properties) throws IOException {
        if (properties.getCommonMessages() == null || properties.getCommonMessages().isEmpty()) {
            return null;
        }
        Properties messages = new Properties();
        for (Resource resource : properties.getCommonMessages()) {
            PropertiesLoaderUtils.fillProperties(messages, resource);
        }
        return messages;
    }

    /**
     * Treats a namespace without bundles as empty, whatever the locale. Bundles come straight from the JDK's bundle
     * cache, which also remembers the ones that are missing and honours {@code spring.messages.cache-duration}.
     */
    private static final class NamespaceBundleMessageSource extends ResourceBundleMessageSource {

        @Override
        protected @Nullable ResourceBundle getResourceBundle(String basename, Locale locale) {
            try {
                return doGetBundle(basename, locale);
            } catch (MissingResourceException namespaceWithoutBundles) {
                return null;
            }
        }
    }
}
