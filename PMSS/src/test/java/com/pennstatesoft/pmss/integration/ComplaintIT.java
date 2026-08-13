package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Complaint lifecycle (/complaints): a client files a complaint (with input
 * validation), an admin lists / responds / re-statuses, and clients are barred
 * from the admin-only actions.
 */
class ComplaintIT extends AbstractIntegrationTest {

    private org.springframework.test.web.servlet.request.RequestPostProcessor asClient() {
        return user(CLIENT_EMAIL).roles("CLIENT");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        return user(ADMIN_EMAIL).roles("ADMINISTRATOR");
    }

    private int complaintCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM Complaints", Integer.class);
    }

    // ---- file (client) ----

    @Test
    void clientFilesComplaint() throws Exception {
        mockMvc.perform(post("/complaints").with(asClient()).with(csrf())
                        .param("complaintOption", "Billing")
                        .param("summary", "I was overcharged for a room."))
                .andExpect(redirectedUrl("/complaints"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(1, complaintCount());
        assertEquals("PENDING", jdbc.queryForObject(
                "SELECT status FROM Complaints WHERE userID = ?", String.class, clientId));
    }

    @Test
    void fileComplaintRejectsBlankSummary() throws Exception {
        mockMvc.perform(post("/complaints").with(asClient()).with(csrf())
                        .param("complaintOption", "Billing")
                        .param("summary", "   "))
                .andExpect(redirectedUrl("/complaints"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(0, complaintCount());
    }

    @Test
    void fileComplaintRejectsInvalidCategory() throws Exception {
        mockMvc.perform(post("/complaints").with(asClient()).with(csrf())
                        .param("complaintOption", "Not A Category")
                        .param("summary", "Something went wrong."))
                .andExpect(redirectedUrl("/complaints"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(0, complaintCount());
    }

    // ---- list / respond / status (admin) ----

    @Test
    void adminListsAllComplaints() throws Exception {
        seedComplaint(clientId, null, "Billing", "Overcharged", "PENDING");

        mockMvc.perform(get("/complaints/list").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("complaints"))
                .andExpect(model().attribute("complaints",
                        hasItem(hasProperty("summary", is("Overcharged")))));
    }

    @Test
    void adminResponseResolvesComplaint() throws Exception {
        int id = seedComplaint(clientId, null, "Billing", "Overcharged", "PENDING");

        mockMvc.perform(post("/complaints/" + id + "/respond").with(asAdmin()).with(csrf())
                        .param("adminResponse", "We have issued a refund."))
                .andExpect(redirectedUrl("/complaints/list"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals("We have issued a refund.", jdbc.queryForObject(
                "SELECT adminResponse FROM Complaints WHERE complaintID = ?", String.class, id));
        assertEquals("RESOLVED", jdbc.queryForObject(
                "SELECT status FROM Complaints WHERE complaintID = ?", String.class, id));
    }

    @Test
    void respondRequiresNonBlankResponse() throws Exception {
        int id = seedComplaint(clientId, null, "Billing", "Overcharged", "PENDING");

        mockMvc.perform(post("/complaints/" + id + "/respond").with(asAdmin()).with(csrf())
                        .param("adminResponse", "  "))
                .andExpect(redirectedUrl("/complaints/" + id))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals("PENDING", jdbc.queryForObject(
                "SELECT status FROM Complaints WHERE complaintID = ?", String.class, id));
    }

    @Test
    void adminUpdatesComplaintStatus() throws Exception {
        int id = seedComplaint(clientId, null, "Billing", "Overcharged", "PENDING");

        mockMvc.perform(post("/complaints/" + id + "/status").with(asAdmin()).with(csrf())
                        .param("newStatus", "IN PROGRESS"))
                .andExpect(redirectedUrl("/complaints/" + id));

        assertEquals("IN PROGRESS", jdbc.queryForObject(
                "SELECT status FROM Complaints WHERE complaintID = ?", String.class, id));
    }

    // ---- authorization: clients cannot use admin actions ----

    @Test
    void clientIsRedirectedAwayFromAdminList() throws Exception {
        mockMvc.perform(get("/complaints/list").with(asClient()))
                .andExpect(redirectedUrl("/complaints"));
    }

    @Test
    void clientCannotRespondToComplaints() throws Exception {
        int id = seedComplaint(clientId, null, "Billing", "Overcharged", "PENDING");

        mockMvc.perform(post("/complaints/" + id + "/respond").with(asClient()).with(csrf())
                        .param("adminResponse", "trying to self-resolve"))
                .andExpect(redirectedUrl("/complaints"));

        // Unchanged — still pending, no admin response stored.
        assertEquals("PENDING", jdbc.queryForObject(
                "SELECT status FROM Complaints WHERE complaintID = ?", String.class, id));
    }
}
