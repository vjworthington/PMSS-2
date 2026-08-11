package com.pennstatesoft.pmss.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final SecurityLogger securityLogger;

    public LoginFailureHandler(SecurityLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("email");

        securityLogger.loginFailure(email);

        response.sendRedirect("/login?error");
    }
}