package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = mock(UserService.class);
    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final RegisterController controller =
            new RegisterController(jdbc, passwordEncoder, userService, securityLogger);

    private void stubEmailCount(int count) {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(count);
    }

    private String flashError(RedirectAttributesModelMap ra) {
        return (String) ra.getFlashAttributes().get("errorMessage");
    }

    // ---- static views ----

    @Test
    void displayFormReturnsRegisterView() {
        assertEquals("register", controller.displayForm());
    }

    @Test
    void displayAdminFormAddsUserAndReturnsView() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@pennstatesoft.com");
        when(userService.findByEmail("admin@pennstatesoft.com")).thenReturn(
                new Client(1, "admin@pennstatesoft.com", "h", "L", "F", "ADMINISTRATOR",
                        "2000-01-01", "F L", null, 0, null, null));
        Model model = new ExtendedModelMap();

        assertEquals("admin/register", controller.displayAdminForm(model, auth));
    }

    // ---- checkEmailUnique ----

    @Test
    void checkEmailUniqueTrueWhenCountZero() {
        stubEmailCount(0);
        assertTrue(controller.checkEmailUnique("new@pennstatesoft.com"));
    }

    @Test
    void checkEmailUniqueFalseWhenCountPositive() {
        stubEmailCount(1);
        assertFalse(controller.checkEmailUnique("taken@pennstatesoft.com"));
    }

    // ---- unsupported factory methods ----

    @Test
    void createClientThrows() {
        assertThrows(UnsupportedOperationException.class, controller::createClient);
    }

    @Test
    void createAdminThrows() {
        assertThrows(UnsupportedOperationException.class, controller::createAdmin);
    }

    // ---- registerAccount ----

    @Test
    void registerAccountFailsWhenEmailTaken() {
        stubEmailCount(1);

        assertFalse(controller.registerAccount("taken@pennstatesoft.com", "pw", null));
        verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void registerAccountInsertsWhenEmailAvailable() {
        stubEmailCount(0);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        assertTrue(controller.registerAccount("new@pennstatesoft.com", "pw", null));
        verify(passwordEncoder).encode("pw");
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---- submitForm validation ----

    @Test
    void submitFormRejectsBlankNames() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitForm("", "", "john@pennstatesoft.com",
                "passw0rd!", "1990-01-01", ra);

        assertEquals("redirect:/register", view);
        assertEquals("First and last name are required.", flashError(ra));
    }

    @Test
    void submitFormRejectsNonCompanyEmail() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitForm("John", "Doe", "john@gmail.com",
                "passw0rd!", "1990-01-01", ra);

        assertEquals("redirect:/register", view);
        assertEquals("Email must end with @pennstatesoft.com.", flashError(ra));
    }

    @Test
    void submitFormRejectsWeakPassword() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitForm("John", "Doe", "john@pennstatesoft.com",
                "allletters", "1990-01-01", ra);

        assertEquals("redirect:/register", view);
        assertEquals("Password must contain at least one number and one special character.",
                flashError(ra));
    }

    @Test
    void submitFormRejectsFutureBirthDate() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitForm("John", "Doe", "john@pennstatesoft.com",
                "passw0rd!", "2999-01-01", ra);

        assertEquals("redirect:/register", view);
        assertEquals("Birth date cannot be in the future.", flashError(ra));
    }

    @Test
    void submitFormRejectsDuplicateEmail() {
        stubEmailCount(1); // not unique
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitForm("John", "Doe", "john@pennstatesoft.com",
                "passw0rd!", "1990-01-01", ra);

        assertEquals("redirect:/register", view);
        assertEquals("An account with that email already exists.", flashError(ra));
    }

    @Test
    void submitFormCreatesClientOnValidInput() {
        stubEmailCount(0); // unique
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitForm("John", "Doe", "John@PennStateSoft.com",
                "passw0rd!", "1990-01-01", ra);

        assertEquals("redirect:/login", view);
        assertEquals("Account created. You can now log in.",
                ra.getFlashAttributes().get("successMessage"));
        // email normalised to lower case before persistence + logging
        verify(securityLogger).clientAccountCreated("john@pennstatesoft.com");
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---- submitAdminForm ----

    @Test
    void submitAdminFormCreatesAdminOnValidUniqueEmail() {
        stubEmailCount(0); // unique / available
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@pennstatesoft.com");
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitAdminForm("Jane", "Roe", "jane@pennstatesoft.com",
                "passw0rd!", "1990-01-01", auth, ra);

        assertEquals("redirect:/admin/register", view);
        assertEquals("Administrator account created for jane@pennstatesoft.com.",
                ra.getFlashAttributes().get("successMessage"));
        verify(securityLogger).adminAccountCreated("admin@pennstatesoft.com", "jane@pennstatesoft.com");
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitAdminFormRejectsDuplicateEmailWithoutInserting() {
        stubEmailCount(1); // not unique / already taken
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitAdminForm("Jane", "Roe", "jane@pennstatesoft.com",
                "passw0rd!", "1990-01-01", auth, ra);

        assertEquals("redirect:/admin/register", view);
        assertEquals("An account with that email already exists.", flashError(ra));
        verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any(), any(), any());
        verify(securityLogger, never()).adminAccountCreated(anyString(), anyString());
    }

    @Test
    void submitAdminFormRejectsInvalidInputWithoutInserting() {
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitAdminForm("", "", "jane@pennstatesoft.com",
                "passw0rd!", "1990-01-01", auth, ra);

        assertEquals("redirect:/admin/register", view);
        assertEquals("First and last name are required.", flashError(ra));
        verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any(), any(), any());
        verify(securityLogger, never()).adminAccountCreated(anyString(), anyString());
    }
}
