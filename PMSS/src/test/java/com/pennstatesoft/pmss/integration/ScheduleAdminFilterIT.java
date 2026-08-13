package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * The administrator schedule dashboard filters (/schedule?filter=...): all / day
 * / week / room / person / timeslot, each verified against a fixed set of three
 * meetings, plus the filter-option model attributes.
 *
 * <p>Fixture:
 * <ul>
 *   <li>Alpha — 2026-09-01 09:00-10:00, regular room, created by the client</li>
 *   <li>Beta  — 2026-09-02 11:00-12:00, special room, created by the client</li>
 *   <li>Gamma — 2026-09-10 09:00-10:00, regular room, created by the admin</li>
 * </ul>
 */
class ScheduleAdminFilterIT extends AbstractIntegrationTest {

    @Override
    protected void seedExtra() {
        seedMeeting("Alpha", "2026-09-01", "09:00", "10:00", clientId, regularRoom);
        seedMeeting("Beta", "2026-09-02", "11:00", "12:00", clientId, specialRoom);
        seedMeeting("Gamma", "2026-09-10", "09:00", "10:00", adminId, regularRoom);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        return user(ADMIN_EMAIL).roles("ADMINISTRATOR");
    }

    @Test
    void defaultFilterShowsAllMeetings() throws Exception {
        mockMvc.perform(get("/schedule").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("schedule"))
                .andExpect(model().attribute("meetings", hasSize(3)));
    }

    @Test
    void adminDashboardExposesFilterOptions() throws Exception {
        mockMvc.perform(get("/schedule").with(asAdmin()))
                .andExpect(model().attributeExists("rooms"))
                .andExpect(model().attributeExists("people"))
                .andExpect(model().attributeExists("timeSlots"));
    }

    @Test
    void filterByDay() throws Exception {
        mockMvc.perform(get("/schedule").param("filter", "day").param("day", "2026-09-01")
                        .with(asAdmin()))
                .andExpect(model().attribute("meetings", hasSize(1)))
                .andExpect(model().attribute("meetings", hasItem(hasProperty("meetingName", is("Alpha")))));
    }

    @Test
    void filterByRoom() throws Exception {
        mockMvc.perform(get("/schedule").param("filter", "room")
                        .param("roomNumber", String.valueOf(regularRoom)).with(asAdmin()))
                .andExpect(model().attribute("meetings", hasSize(2)))
                .andExpect(model().attribute("meetings", hasItem(hasProperty("meetingName", is("Alpha")))))
                .andExpect(model().attribute("meetings", hasItem(hasProperty("meetingName", is("Gamma")))))
                .andExpect(model().attribute("meetings", not(hasItem(hasProperty("meetingName", is("Beta"))))));
    }

    @Test
    void filterByWeek() throws Exception {
        // Week of Mon 2026-08-31 .. Sun 2026-09-06 contains Alpha and Beta, not Gamma.
        mockMvc.perform(get("/schedule").param("filter", "week").param("weekStart", "2026-09-01")
                        .with(asAdmin()))
                .andExpect(model().attribute("meetings", hasSize(2)))
                .andExpect(model().attribute("meetings", not(hasItem(hasProperty("meetingName", is("Gamma"))))));
    }

    @Test
    void filterByTimeslot() throws Exception {
        // The 09:00-10:00 slot overlaps Alpha and Gamma, not Beta (11:00-12:00).
        mockMvc.perform(get("/schedule").param("filter", "timeslot").param("timeSlot", "09:00")
                        .with(asAdmin()))
                .andExpect(model().attribute("meetings", hasSize(2)))
                .andExpect(model().attribute("meetings", hasItem(hasProperty("meetingName", is("Alpha")))))
                .andExpect(model().attribute("meetings", hasItem(hasProperty("meetingName", is("Gamma")))));
    }

    @Test
    void filterByPerson() throws Exception {
        // Only the admin-created Gamma belongs to the admin.
        mockMvc.perform(get("/schedule").param("filter", "person")
                        .param("personID", String.valueOf(adminId)).with(asAdmin()))
                .andExpect(model().attribute("meetings", hasSize(1)))
                .andExpect(model().attribute("meetings", hasItem(hasProperty("meetingName", is("Gamma")))));
    }
}
