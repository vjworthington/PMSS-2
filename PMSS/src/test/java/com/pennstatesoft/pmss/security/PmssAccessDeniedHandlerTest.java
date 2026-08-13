package com.pennstatesoft.pmss.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PmssAccessDeniedHandlerTest {

    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final PmssAccessDeniedHandler handler = new PmssAccessDeniedHandler(securityLogger);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsAuthenticatedUserAndSends403() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("a@x.com", null, List.of()));
        when(request.getRequestURI()).thenReturn("/admin/secret");

        handler.handle(request, response, new AccessDeniedException("nope"));

        verify(securityLogger).authorizationFailure("a@x.com", "/admin/secret");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void logsAnonymousWhenNoAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        when(request.getRequestURI()).thenReturn("/admin/secret");

        handler.handle(request, response, new AccessDeniedException("nope"));

        verify(securityLogger).authorizationFailure("anonymous", "/admin/secret");
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
