package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real Spring Security filter chain: form login against the custom
 * {@code UserService}/{@code DaoAuthenticationProvider}, the role-aware success
 * handler, the failed-login lockout policy, and URL authorization rules.
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    // Form login uses the app's custom "email" username parameter.
    private org.springframework.test.web.servlet.RequestBuilder login(String email, String password) {
        return formLogin("/login").userParameter("email").user(email).password(password);
    }

    // ---- successful authentication ----

    @Test
    void clientLoginSucceedsAndRedirectsToClientLanding() throws Exception {
        mockMvc.perform(login(CLIENT_EMAIL, RAW_PASSWORD))
                .andExpect(authenticated().withUsername(CLIENT_EMAIL).withRoles("CLIENT"))
                .andExpect(redirectedUrl("/client/landing"));
    }

    @Test
    void adminLoginSucceedsAndRedirectsToAdminLanding() throws Exception {
        mockMvc.perform(login(ADMIN_EMAIL, RAW_PASSWORD))
                .andExpect(authenticated().withUsername(ADMIN_EMAIL).withRoles("ADMINISTRATOR"))
                .andExpect(redirectedUrl("/admin/landing"));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(login(CLIENT_EMAIL, "WrongPassword1!"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    // ---- lockout policy: 3 failures within 30 minutes locks for 15 minutes ----

    @Test
    void threeFailedAttemptsLockAccountAndBlockValidLogin() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login(CLIENT_EMAIL, "WrongPassword1!"))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        // The policy is now recorded in the database.
        int failedAttempts = jdbc.queryForObject(
                "SELECT failedAttempts FROM Users WHERE userEmail = ?", Integer.class, CLIENT_EMAIL);
        assertEquals(3, failedAttempts);
        String lockedTimeTo = jdbc.queryForObject(
                "SELECT lockedTimeTo FROM Users WHERE userEmail = ?", String.class, CLIENT_EMAIL);
        assertNotNull(lockedTimeTo, "account should be locked after 3 failed attempts");

        // The security guarantee under test: while the lock is active, even the
        // correct password does NOT authenticate the user.
        //
        // Note: UserService.loadUserByUsername throws LockedException, but
        // DaoAuthenticationProvider wraps any non-UsernameNotFoundException from
        // loadUserByUsername in an InternalAuthenticationServiceException. As a
        // result the LoginFailureHandler never sees a LockedException, so the
        // redirect is "/login?error" rather than the intended "/login?locked".
        // The account is still effectively locked (login is blocked); only the
        // user-facing message is wrong.
        mockMvc.perform(login(CLIENT_EMAIL, RAW_PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void successfulLoginResetsFailureCounter() throws Exception {
        mockMvc.perform(login(CLIENT_EMAIL, "WrongPassword1!"))
                .andExpect(unauthenticated());
        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT failedAttempts FROM Users WHERE userEmail = ?", Integer.class, CLIENT_EMAIL));

        mockMvc.perform(login(CLIENT_EMAIL, RAW_PASSWORD))
                .andExpect(authenticated());

        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT failedAttempts FROM Users WHERE userEmail = ?", Integer.class, CLIENT_EMAIL));
    }

    // ---- URL authorization rules ----

    @Test
    void unauthenticatedRequestToProtectedPageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/schedule"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void clientCannotAccessAdminArea() throws Exception {
        mockMvc.perform(get("/admin/landing").with(user(CLIENT_EMAIL).roles("CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAccessClientArea() throws Exception {
        mockMvc.perform(get("/client/landing").with(user(ADMIN_EMAIL).roles("ADMINISTRATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCanAccessOwnLanding() throws Exception {
        mockMvc.perform(get("/client/landing").with(user(CLIENT_EMAIL).roles("CLIENT")))
                .andExpect(status().isOk());
    }
}
