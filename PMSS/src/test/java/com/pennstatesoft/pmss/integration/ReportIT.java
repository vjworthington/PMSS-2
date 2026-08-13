package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Administrator reporting (/admin/reports and the CSV exports): the dashboard
 * aggregates, CSV headers/content and download headers, the users export
 * excluding sensitive fields, and ADMINISTRATOR-only access.
 */
class ReportIT extends AbstractIntegrationTest {

    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        return user(ADMIN_EMAIL).roles("ADMINISTRATOR");
    }

    @Override
    protected void seedExtra() {
        seedMeeting("Quarterly Review", "2026-09-01", "09:00", "10:00", clientId, regularRoom);
        seedComplaint(clientId, null, "Billing", "Overcharged", "PENDING");
    }

    // ---- dashboard ----

    @Test
    void adminSeesReportAggregates() throws Exception {
        mockMvc.perform(get("/admin/reports").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attribute("totalMeetings", is(1)))
                .andExpect(model().attribute("totalRooms", is(2)))
                .andExpect(model().attribute("specialRooms", is(1)))
                .andExpect(model().attribute("totalClients", is(1)))
                .andExpect(model().attribute("totalAdmins", is(1)))
                .andExpect(model().attribute("totalComplaints", is(1)));
    }

    // ---- CSV exports ----

    @Test
    void meetingsCsvHasHeaderAndData() throws Exception {
        mockMvc.perform(get("/admin/reports/meetings.csv").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("meetings.csv")))
                .andExpect(content().string(containsString(
                        "meetingID,meetingName,meetingDate,startTime,endTime,roomNumber,creator,status")))
                .andExpect(content().string(containsString("Quarterly Review")));
    }

    @Test
    void complaintsCsvHasHeaderAndData() throws Exception {
        mockMvc.perform(get("/admin/reports/complaints.csv").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("complaintID,client,meetingID")))
                .andExpect(content().string(containsString("Overcharged")));
    }

    @Test
    void roomsCsvHasHeader() throws Exception {
        mockMvc.perform(get("/admin/reports/rooms.csv").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("roomNumber,roomType,fee,meetings")));
    }

    @Test
    void usersCsvExcludesSensitiveFields() throws Exception {
        String body = mockMvc.perform(get("/admin/reports/users.csv").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("userID,firstName,lastName,userEmail,role,displayName,birthDate")))
                .andExpect(content().string(containsString(CLIENT_EMAIL)))
                .andReturn().getResponse().getContentAsString();

        // Password hashes and image blobs must never leak into the export.
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("passwordHash"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("$2"),
                "BCrypt hashes must not appear in the users export");
    }

    // ---- authorization ----

    @Test
    void clientCannotAccessReportsDashboard() throws Exception {
        mockMvc.perform(get("/admin/reports").with(user(CLIENT_EMAIL).roles("CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCannotDownloadCsv() throws Exception {
        mockMvc.perform(get("/admin/reports/meetings.csv").with(user(CLIENT_EMAIL).roles("CLIENT")))
                .andExpect(status().isForbidden());
    }
}
