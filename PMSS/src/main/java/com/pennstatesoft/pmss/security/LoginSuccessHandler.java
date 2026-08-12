package com.pennstatesoft.pmss.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import java.io.IOException;
import com.pennstatesoft.pmss.service.UserService;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SecurityLogger securityLogger;
    private final UserService userService;

    public LoginSuccessHandler(SecurityLogger securityLogger, UserService userService) {
        this.securityLogger = securityLogger;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        String email = authentication.getName();

        userService.resetLoginFailures(email);

        securityLogger.loginSuccess(email);

        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"))) {

            response.sendRedirect("/admin/landing");

        } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"))) {

            response.sendRedirect("/client/landing");

        } else {

            response.sendRedirect("/login?error");
        }
    }
}