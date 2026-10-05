package com.myagree.app.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** The signed-in user of the request being served, for services; controllers declare a {@link CurrentUser} parameter. */
@Component
public class CurrentUserProvider {

    /**
     * @throws IllegalStateException when no user is signed in, i.e. when called while serving a public endpoint
     */
    public CurrentUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return AccessTokenClaims.decode(token.getToken());
        }
        throw new IllegalStateException("No signed-in user: the current user is only available on authenticated endpoints");
    }
}
