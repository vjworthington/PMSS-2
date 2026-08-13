package com.pennstatesoft.pmss.config;

import com.pennstatesoft.pmss.security.LoginFailureHandler;
import com.pennstatesoft.pmss.security.LoginSuccessHandler;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig(
            mock(UserService.class),
            mock(SecurityLogger.class),
            mock(LoginSuccessHandler.class),
            mock(LoginFailureHandler.class));

    @Test
    void passwordEncoderIsBcryptAndVerifies() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        String hash = encoder.encode("secret123!");
        assertTrue(encoder.matches("secret123!", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void authenticationProviderIsCreated() {
        DaoAuthenticationProvider provider = config.authenticationProvider();
        assertNotNull(provider);
    }
}
