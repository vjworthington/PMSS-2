package com.pennstatesoft.pmss.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class PmssAccessDeniedHandler
        implements AccessDeniedHandler {

    private final SecurityLogger securityLogger;

    public PmssAccessDeniedHandler(SecurityLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication != null
                ? authentication.getName()
                : "anonymous";

        securityLogger.authorizationFailure(username, request.getRequestURI()
        );

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}