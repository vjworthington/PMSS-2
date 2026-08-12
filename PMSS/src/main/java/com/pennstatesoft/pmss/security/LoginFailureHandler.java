package com.pennstatesoft.pmss.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import com.pennstatesoft.pmss.service.UserService;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final SecurityLogger securityLogger;
    private final UserService userService;

    public LoginFailureHandler(SecurityLogger securityLogger, UserService userService) {
        this.securityLogger = securityLogger;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("email");

        securityLogger.loginFailure(email);

        if (exception instanceof LockedException) {

            response.sendRedirect("/login?locked");

        } else {

            if (email != null && !email.isBlank()) {
                userService.recordFailedLogin(email);
            }

            response.sendRedirect("/login?error");
        }
    }

}