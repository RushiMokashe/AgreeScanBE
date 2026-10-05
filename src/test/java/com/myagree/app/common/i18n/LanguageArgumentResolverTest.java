package com.myagree.app.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class LanguageArgumentResolverTest {

    private final LanguageArgumentResolver resolver = new LanguageArgumentResolver();

    @Test
    void resolvesLanguageParametersOnly() throws NoSuchMethodException {
        Method handler = Handlers.class.getDeclaredMethod("handle", Language.class, String.class);

        assertThat(resolver.supportsParameter(new MethodParameter(handler, 0))).isTrue();
        assertThat(resolver.supportsParameter(new MethodParameter(handler, 1))).isFalse();
    }

    @Test
    void givesTheRequestsAcceptLanguage() throws NoSuchMethodException {
        MethodParameter language = new MethodParameter(Handlers.class.getDeclaredMethod("handle", Language.class, String.class), 0);
        MockHttpServletRequest marathi = new MockHttpServletRequest();
        marathi.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "mr-IN,en;q=0.5");

        assertThat(resolver.resolveArgument(language, null, new ServletWebRequest(marathi), null)).isEqualTo(Language.MR);
        assertThat(resolver.resolveArgument(language, null, new ServletWebRequest(new MockHttpServletRequest()), null))
                .isEqualTo(Language.EN);
    }

    /** A controller method as features write them. */
    private static final class Handlers {

        @SuppressWarnings("unused")
        void handle(Language language, String other) {
        }
    }
}
