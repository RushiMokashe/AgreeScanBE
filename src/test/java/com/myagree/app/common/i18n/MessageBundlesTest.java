package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.myagree.app.support.AgriScanApiTest;

/**
 * The server-side message bundles as configured in {@code spring.messages.basename}. Every namespace that ships its
 * English base bundle must translate each message into Marathi and Hindi with the same placeholders.
 */
@AgriScanApiTest
class MessageBundlesTest {

    private static final List<String> TRANSLATIONS = List.of("_mr", "_hi");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)[^}]*}");
    /** An apostrophe on its own: MessageFormat reads it as a quote and swallows the text that follows. */
    private static final Pattern LONE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");

    @Autowired
    private Messages messages;

    @Autowired
    private Environment environment;

    @Test
    void platformTextsResolveInEveryLanguage() {
        assertThat(messages.get("common.auth.bad-credentials", Language.EN)).isEqualTo("Incorrect phone number or password");
        assertThat(messages.get("common.auth.bad-credentials", Language.MR)).isEqualTo("फोन नंबर किंवा पासवर्ड चुकीचा आहे");
        assertThat(messages.get("common.auth.bad-credentials", Language.HI)).isEqualTo("फ़ोन नंबर या पासवर्ड गलत है");
        assertThat(messages.get("common.account.phone-taken", Language.MR, "9876543210"))
                .isEqualTo("9876543210 हा फोन नंबर आधीच नोंदणीकृत आहे");
    }

    @Test
    void namespacesWithoutBundlesYetAreSkipped() {
        assertThat(basenames()).contains("i18n/common", "i18n/farm", "i18n/market", "i18n/rental", "i18n/store",
                "i18n/payment", "i18n/admin", "i18n/assistant", "i18n/notification");
        assertThatThrownBy(() -> messages.get("admin.no-such-message", Language.HI))
                .isInstanceOf(NoSuchMessageException.class);
    }

    @Test
    void everyBundleIsFullyTranslatedWithTheSamePlaceholders() throws IOException {
        for (String basename : basenames()) {
            Properties english = load(basename + ".properties");
            for (String suffix : TRANSLATIONS) {
                Properties translation = load(basename + suffix + ".properties");
                assertThat(translation.stringPropertyNames())
                        .as("messages of %s%s", basename, suffix)
                        .isEqualTo(english.stringPropertyNames());
                for (String code : english.stringPropertyNames()) {
                    assertThat(placeholders(translation.getProperty(code)))
                            .as("placeholders of %s in %s%s", code, basename, suffix)
                            .isEqualTo(placeholders(english.getProperty(code)));
                }
            }
        }
    }

    @Test
    void messagesWithPlaceholdersAreValidMessageFormats() throws IOException {
        for (String basename : basenames()) {
            for (String suffix : List.of("", "_mr", "_hi")) {
                Properties bundle = load(basename + suffix + ".properties");
                for (String code : bundle.stringPropertyNames()) {
                    String message = bundle.getProperty(code);
                    if (placeholders(message).isEmpty()) {
                        continue;
                    }
                    assertThat(LONE_APOSTROPHE.matcher(message).find())
                            .as("%s in %s%s writes a literal apostrophe as ''", code, basename, suffix)
                            .isFalse();
                    assertThatNoException().as("%s in %s%s", code, basename, suffix)
                            .isThrownBy(() -> new MessageFormat(message));
                }
            }
        }
    }

    /** Every configured namespace; one whose feature has no texts yet has no files, which reads as empty bundles. */
    private List<String> basenames() {
        return Arrays.stream(environment.getRequiredProperty("spring.messages.basename", String[].class))
                .map(String::strip)
                .toList();
    }

    private static Properties load(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.exists()
                ? PropertiesLoaderUtils.loadProperties(new EncodedResource(resource, StandardCharsets.UTF_8))
                : new Properties();
    }

    private static Set<String> placeholders(String message) {
        Matcher matcher = PLACEHOLDER.matcher(message);
        return matcher.results().map(result -> result.group(1)).collect(Collectors.toSet());
    }
}
