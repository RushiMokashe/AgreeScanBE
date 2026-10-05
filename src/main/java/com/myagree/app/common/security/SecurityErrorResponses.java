package com.myagree.app.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.myagree.app.common.ApiError;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.common.i18n.UserMessage;

import tools.jackson.databind.json.JsonMapper;

/**
 * Answers requests the security rules turn away with the {@link ApiError} body every other failure uses, in the
 * request's language: 401 without a valid access token, 403 when the user's roles do not allow the request. The
 * RFC 6750 {@code WWW-Authenticate} challenge is kept.
 */
final class SecurityErrorResponses implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final UserMessage SIGN_IN_REQUIRED = UserMessage.of("common.auth.sign-in-required");
    private static final UserMessage SESSION_EXPIRED = UserMessage.of("common.auth.session-expired");
    private static final UserMessage ACCESS_DENIED = UserMessage.of("common.auth.access-denied");

    private final AuthenticationEntryPoint bearerChallenge = new BearerTokenAuthenticationEntryPoint();
    private final AccessDeniedHandler bearerDenial = new BearerTokenAccessDeniedHandler();
    private final JsonMapper jsonMapper;
    private final Messages messages;

    SecurityErrorResponses(JsonMapper jsonMapper, Messages messages) {
        this.jsonMapper = jsonMapper;
        this.messages = messages;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        bearerChallenge.commence(request, response, exception);
        // A token was sent but is expired, forged or malformed; otherwise none was sent at all.
        write(request, response, exception instanceof OAuth2AuthenticationException ? SESSION_EXPIRED : SIGN_IN_REQUIRED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException, ServletException {
        bearerDenial.handle(request, response, exception);
        write(request, response, ACCESS_DENIED);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, UserMessage message) throws IOException {
        Language language = Language.fromAcceptLanguage(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        jsonMapper.writeValue(response.getOutputStream(),
                ApiError.of(HttpStatusCode.valueOf(response.getStatus()), messages.get(message, language)));
    }
}
