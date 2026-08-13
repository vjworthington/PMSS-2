package com.pennstatesoft.pmss.security;

import com.pennstatesoft.pmss.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginFailureHandlerTest {

    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final UserService userService = mock(UserService.class);
    private final LoginFailureHandler handler = new LoginFailureHandler(securityLogger, userService);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void lockedExceptionRedirectsToLockedAndSkipsRecording() throws Exception {
        when(request.getParameter("email")).thenReturn("a@x.com");

        handler.onAuthenticationFailure(request, response, new LockedException("locked"));

        verify(securityLogger).loginFailure("a@x.com");
        verify(response).sendRedirect("/login?locked");
        verify(userService, never()).recordFailedLogin(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void badCredentialsRecordsFailureAndRedirectsToError() throws Exception {
        when(request.getParameter("email")).thenReturn("a@x.com");

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(userService).recordFailedLogin("a@x.com");
        verify(response).sendRedirect("/login?error");
    }

    @Test
    void blankEmailDoesNotRecordFailure() throws Exception {
        when(request.getParameter("email")).thenReturn(null);

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(userService, never()).recordFailedLogin(org.mockito.ArgumentMatchers.any());
        verify(response).sendRedirect("/login?error");
    }
}
