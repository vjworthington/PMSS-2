package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserService userService = mock(UserService.class);
    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final BillingController controller = new BillingController(jdbc, userService, securityLogger);

    private User client() {
        return new Client(1, "c@pennstatesoft.com", "h", "L", "F", "CLIENT",
                "2000-01-01", "F L", null, 0, null, null);
    }

    private Authentication auth() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("c@pennstatesoft.com");
        when(userService.findByEmail("c@pennstatesoft.com")).thenReturn(client());
        return a;
    }

    private String error(RedirectAttributesModelMap ra) {
        return (String) ra.getFlashAttributes().get("errorMessage");
    }

    @Test
    void viewBillingLoadsCard() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.viewBilling(model, auth());

        assertEquals("billing", view);
    }

    @Test
    void saveBillingRejectsBlankCardholder() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.saveBilling("", "Visa", "4111111111111111", "12/28", auth(), ra);

        assertEquals("redirect:/billing", view);
        assertEquals("Cardholder name is required.", error(ra));
        verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void saveBillingRejectsShortCardNumber() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.saveBilling("John Doe", "Visa", "4111", "12/28", auth(), ra);

        assertEquals("Enter a valid card number.", error(ra));
    }

    @Test
    void saveBillingRejectsBadExpiry() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.saveBilling("John Doe", "Visa", "4111111111111111", "2028-12", auth(), ra);

        assertEquals("Enter the expiration date as MM/YY.", error(ra));
    }

    @Test
    void saveBillingStoresMaskedCard() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.saveBilling("John Doe", "Visa", "4111 1111 1111 1234", "12/28", auth(), ra);

        assertEquals("redirect:/billing", view);
        assertEquals("Payment information updated.", ra.getFlashAttributes().get("successMessage"));
        // Only masked data persisted: last 4 digits, never the full number.
        verify(jdbc).update(anyString(), eq(1), eq("John Doe"), eq("Visa"), eq("1234"), eq("12/28"));
        verify(securityLogger).billingUpdated("c@pennstatesoft.com", 1);
    }

    @Test
    void saveBillingDefaultsBlankCardTypeToCard() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        controller.saveBilling("John Doe", "  ", "4111111111111234", "12/28", auth(), ra);

        verify(jdbc).update(anyString(), eq(1), eq("John Doe"), eq("Card"), eq("1234"), eq("12/28"));
    }

    @Test
    void adminEditBillingRedirectsWhenClientMissing() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = controller.adminEditBilling(99, model, auth(), ra);

        assertEquals("redirect:/admin/billing", view);
        assertEquals("Client not found.", error(ra));
    }

    @Test
    void adminEditBillingShowsEditPageWhenClientExists() {
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("userID", 5, "firstName", "Jane")))
                .thenReturn(List.of()); // second call: findCard
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = controller.adminEditBilling(5, model, auth(), ra);

        assertEquals("admin/billing-edit", view);
    }

    @Test
    void adminUpdateBillingRejectsInvalidCardAndRedirectsToEdit() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.adminUpdateBilling(5, "John Doe", "Visa", "123", "12/28", auth(), ra);

        assertEquals("redirect:/admin/billing/5/edit", view);
        assertEquals("Enter a valid card number.", error(ra));
    }

    @Test
    void adminUpdateBillingStoresCard() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.adminUpdateBilling(5, "John Doe", "Amex", "371449635398431", "01/27", auth(), ra);

        assertEquals("redirect:/admin/billing", view);
        verify(jdbc).update(anyString(), eq(5), eq("John Doe"), eq("Amex"), eq("8431"), eq("01/27"));
        verify(securityLogger).billingUpdated("c@pennstatesoft.com", 5);
    }
}
