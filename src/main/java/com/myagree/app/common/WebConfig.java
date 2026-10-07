package com.myagree.app.common;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.myagree.app.common.i18n.LanguageArgumentResolver;
import com.myagree.app.common.security.CurrentUserArgumentResolver;
import com.myagree.app.common.security.CurrentUserProvider;
import com.myagree.app.common.security.NativeClient;

/**
 * Controller parameters every feature may declare ({@code Language}, {@code CurrentUser}) and CORS. The Vite dev
 * server normally proxies {@code /api}; CORS, which the security filter chain also applies, covers direct calls.
 */
@Configuration(proxyBeanMethods = false)
class WebConfig implements WebMvcConfigurer {

    private final AgriScanProperties properties;
    private final CurrentUserProvider currentUserProvider;

    WebConfig(AgriScanProperties properties, CurrentUserProvider currentUserProvider) {
        this.properties = properties;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // The Vite dev server and the installed app call the API from other origins; the app also reads the refresh
        // token from a response header (NativeClient).
        registry.addMapping("/api/**")
                .allowedOrigins(properties.corsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .exposedHeaders(NativeClient.REFRESH_TOKEN_HEADER);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LanguageArgumentResolver());
        resolvers.add(new CurrentUserArgumentResolver(currentUserProvider));
    }
}
