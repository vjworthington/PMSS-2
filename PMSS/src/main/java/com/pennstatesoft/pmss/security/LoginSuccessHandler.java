package com.pennstatesoft.pmss.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class LoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final SecurityLogger securityLogger;

    public LoginSuccessHandler(SecurityLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        securityLogger.loginSuccess(authentication.getName());

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_ADMINISTRATOR"))) {

            response.sendRedirect("/admin/landing");

        } else if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_CLIENT"))) {

            response.sendRedirect("/client/landing");

        } else {
            response.sendRedirect("/login?error");
        }
    }
}