package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end registration: the HTTP form POST flows through the controller,
 * validation, the password encoder, and the JDBC layer into the database.
 */
class RegistrationIntegrationTest extends AbstractIntegrationTest {

    private static final String NEW_EMAIL = "newuser@pennstatesoft.com";

    private int userCount(String email) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM Users WHERE userEmail = ?", Integer.class, email);
    }

    @Test
    void registersNewClientAndPersistsBcryptHash() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "New")
                        .param("lastName", "User")
                        .param("email", NEW_EMAIL)
                        .param("password", "Password1!")
                        .param("birthDate", "1990-05-05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", notNullValue()));

        assertEquals(1, userCount(NEW_EMAIL));

        String role = jdbc.queryForObject(
                "SELECT role FROM Users WHERE userEmail = ?", String.class, NEW_EMAIL);
        assertEquals("CLIENT", role);

        String hash = jdbc.queryForObject(
                "SELECT passwordHash FROM Users WHERE userEmail = ?", String.class, NEW_EMAIL);
        assertTrue(hash.startsWith("$2"), "password must be stored BCrypt-hashed, never in plain text");
        assertTrue(passwordEncoder.matches("Password1!", hash));
    }

    @Test
    void normalizesEmailToLowercaseBeforePersisting() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "Mixed")
                        .param("lastName", "Case")
                        .param("email", "MixedCase@PennStateSoft.com")
                        .param("password", "Password1!")
                        .param("birthDate", "1990-05-05"))
                .andExpect(redirectedUrl("/login"));

        assertEquals(1, userCount("mixedcase@pennstatesoft.com"));
    }

    @Test
    void rejectsWeakPasswordWithoutPersisting() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "Weak")
                        .param("lastName", "Pass")
                        .param("email", "weak@pennstatesoft.com")
                        .param("password", "password")   // no digit, no special char
                        .param("birthDate", "1990-05-05"))
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", notNullValue()));

        assertEquals(0, userCount("weak@pennstatesoft.com"));
    }

    @Test
    void rejectsEmailOutsideCompanyDomain() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "Outside")
                        .param("lastName", "Domain")
                        .param("email", "someone@gmail.com")
                        .param("password", "Password1!")
                        .param("birthDate", "1990-05-05"))
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", notNullValue()));

        assertEquals(0, userCount("someone@gmail.com"));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        // CLIENT_EMAIL is already seeded by the base class.
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "Dupe")
                        .param("lastName", "Client")
                        .param("email", CLIENT_EMAIL)
                        .param("password", "Password1!")
                        .param("birthDate", "1990-05-05"))
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", notNullValue()));

        // Still exactly one account with that email.
        assertEquals(1, userCount(CLIENT_EMAIL));
    }

    // ---- admin-initiated registration (/admin/register) ----

    private static final String NEW_ADMIN_EMAIL = "newadmin@pennstatesoft.com";

    @Test
    void adminCreatesAdministratorAccount() throws Exception {
        mockMvc.perform(post("/admin/register")
                        .with(user(ADMIN_EMAIL).roles("ADMINISTRATOR")).with(csrf())
                        .param("firstName", "New")
                        .param("lastName", "Admin")
                        .param("email", NEW_ADMIN_EMAIL)
                        .param("password", "Password1!")
                        .param("birthDate", "1985-03-03"))
                .andExpect(redirectedUrl("/admin/register"))
                .andExpect(flash().attribute("successMessage", notNullValue()));

        assertEquals(1, userCount(NEW_ADMIN_EMAIL));
        assertEquals("ADMINISTRATOR", jdbc.queryForObject(
                "SELECT role FROM Users WHERE userEmail = ?", String.class, NEW_ADMIN_EMAIL));
    }

    @Test
    void adminRegisterRejectsInvalidInputWithoutCreatingAccount() throws Exception {
        mockMvc.perform(post("/admin/register")
                        .with(user(ADMIN_EMAIL).roles("ADMINISTRATOR")).with(csrf())
                        .param("firstName", "")   // blank name fails validation
                        .param("lastName", "")
                        .param("email", NEW_ADMIN_EMAIL)
                        .param("password", "Password1!")
                        .param("birthDate", "1985-03-03"))
                .andExpect(redirectedUrl("/admin/register"))
                .andExpect(flash().attribute("errorMessage", notNullValue()));

        assertEquals(0, userCount(NEW_ADMIN_EMAIL));
    }

    @Test
    void adminRegisterRejectsDuplicateEmailWithoutCreatingAccount() throws Exception {
        // ADMIN_EMAIL is already seeded.
        mockMvc.perform(post("/admin/register")
                        .with(user(ADMIN_EMAIL).roles("ADMINISTRATOR")).with(csrf())
                        .param("firstName", "Dupe")
                        .param("lastName", "Admin")
                        .param("email", ADMIN_EMAIL)
                        .param("password", "Password1!")
                        .param("birthDate", "1985-03-03"))
                .andExpect(redirectedUrl("/admin/register"))
                .andExpect(flash().attribute("errorMessage", notNullValue()));

        // Still exactly one account with that email.
        assertEquals(1, userCount(ADMIN_EMAIL));
    }
}
