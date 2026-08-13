package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Billing / card-on-file (/billing and /admin/billing): validation, the masked
 * "only last 4 digits are stored" rule, upsert-on-save, and admin access.
 */
class BillingIT extends AbstractIntegrationTest {

    private static final String FULL_PAN = "4111111111111234";

    private org.springframework.test.web.servlet.request.RequestPostProcessor asClient() {
        return user(CLIENT_EMAIL).roles("CLIENT");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        return user(ADMIN_EMAIL).roles("ADMINISTRATOR");
    }

    private Map<String, Object> card(int userID) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM Billing WHERE userID = ?", userID);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ---- client ----

    @Test
    void clientViewsBillingPage() throws Exception {
        mockMvc.perform(get("/billing").with(asClient()))
                .andExpect(status().isOk())
                .andExpect(view().name("billing"));
    }

    @Test
    void clientSavesCardAndOnlyLast4IsStored() throws Exception {
        mockMvc.perform(post("/billing").with(asClient()).with(csrf())
                        .param("cardholderName", "Casey Client")
                        .param("cardType", "Visa")
                        .param("cardNumber", "4111 1111 1111 1234")
                        .param("cardExpiry", "12/28"))
                .andExpect(redirectedUrl("/billing"))
                .andExpect(flash().attributeExists("successMessage"));

        Map<String, Object> row = card(clientId);
        assertEquals("Casey Client", row.get("cardholderName"));
        assertEquals("Visa", row.get("cardType"));
        assertEquals("1234", row.get("cardLast4"));

        // The full card number must never be persisted in any column.
        for (Object value : row.values()) {
            assertTrue(value == null || !value.toString().contains(FULL_PAN),
                    "no column may store the full card number");
        }
    }

    @Test
    void savingCardTwiceUpsertsInsteadOfDuplicating() throws Exception {
        for (String cardNumber : new String[]{"4111111111111234", "4111111111119876"}) {
            mockMvc.perform(post("/billing").with(asClient()).with(csrf())
                            .param("cardholderName", "Casey Client")
                            .param("cardType", "Visa")
                            .param("cardNumber", cardNumber)
                            .param("cardExpiry", "12/28"))
                    .andExpect(redirectedUrl("/billing"));
        }

        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM Billing WHERE userID = ?", Integer.class, clientId));
        assertEquals("9876", card(clientId).get("cardLast4"));
    }

    @Test
    void saveBillingRejectsShortCardNumber() throws Exception {
        mockMvc.perform(post("/billing").with(asClient()).with(csrf())
                        .param("cardholderName", "Casey Client")
                        .param("cardType", "Visa")
                        .param("cardNumber", "4111")
                        .param("cardExpiry", "12/28"))
                .andExpect(redirectedUrl("/billing"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertNull(card(clientId));
    }

    @Test
    void saveBillingRejectsBadExpiryFormat() throws Exception {
        mockMvc.perform(post("/billing").with(asClient()).with(csrf())
                        .param("cardholderName", "Casey Client")
                        .param("cardType", "Visa")
                        .param("cardNumber", "4111 1111 1111 1234")
                        .param("cardExpiry", "2028-12"))
                .andExpect(redirectedUrl("/billing"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertNull(card(clientId));
    }

    // ---- admin ----

    @Test
    void adminViewsClientBillingList() throws Exception {
        mockMvc.perform(get("/admin/billing").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/billing"));
    }

    @Test
    void adminUpdatesClientBilling() throws Exception {
        mockMvc.perform(post("/admin/billing/" + clientId + "/edit").with(asAdmin()).with(csrf())
                        .param("cardholderName", "Casey Client")
                        .param("cardType", "Amex")
                        .param("cardNumber", "3782 822463 10005")
                        .param("cardExpiry", "01/30"))
                .andExpect(redirectedUrl("/admin/billing"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals("0005", card(clientId).get("cardLast4"));
        assertEquals("Amex", card(clientId).get("cardType"));
    }

    @Test
    void adminEditUnknownClientRedirectsWithError() throws Exception {
        mockMvc.perform(get("/admin/billing/99999/edit").with(asAdmin()))
                .andExpect(redirectedUrl("/admin/billing"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void clientCannotAccessAdminBilling() throws Exception {
        mockMvc.perform(get("/admin/billing").with(asClient()))
                .andExpect(status().isForbidden());
    }
}
