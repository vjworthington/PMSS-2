package com.pennstatesoft.pmss.security;

import com.pennstatesoft.pmss.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoginSuccessHandlerTest {

    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final UserService userService = mock(UserService.class);
    private final LoginSuccessHandler handler = new LoginSuccessHandler(securityLogger, userService);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    private Authentication auth(String email, String role) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    @Test
    void adminRedirectedToAdminLanding() throws IOException {
        handler.onAuthenticationSuccess(request, response, auth("admin@x.com", "ROLE_ADMINISTRATOR"));

        verify(userService).resetLoginFailures("admin@x.com");
        verify(securityLogger).loginSuccess("admin@x.com");
        verify(response).sendRedirect("/admin/landing");
    }

    @Test
    void clientRedirectedToClientLanding() throws IOException {
        handler.onAuthenticationSuccess(request, response, auth("client@x.com", "ROLE_CLIENT"));

        verify(response).sendRedirect("/client/landing");
    }

    @Test
    void unknownRoleRedirectedToLoginError() throws IOException {
        handler.onAuthenticationSuccess(request, response, auth("weird@x.com", "ROLE_OTHER"));

        verify(response).sendRedirect("/login?error");
    }
}
