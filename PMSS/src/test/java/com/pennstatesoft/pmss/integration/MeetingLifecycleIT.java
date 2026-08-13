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
 * Full meeting lifecycle over HTTP: create (with business-hour, room-conflict and
 * double-booking rules), the special-room payment confirmation gate, listing on
 * the schedule, creator-only edit/delete, and the resulting database state.
 */
class MeetingLifecycleIT extends AbstractIntegrationTest {

    private static final String DATE = "2026-09-01";

    private org.springframework.test.web.servlet.request.RequestPostProcessor asClient() {
        return user(CLIENT_EMAIL).roles("CLIENT");
    }

    private int meetingId(String name) {
        return jdbc.queryForObject(
                "SELECT meetingID FROM Meetings WHERE meetingName = ?", Integer.class, name);
    }

    // ---- create ----

    @Test
    void clientCreatesMeetingInFreeRegularRoom() throws Exception {
        mockMvc.perform(post("/meetings/create").with(asClient()).with(csrf())
                        .param("meetingName", "Sprint Planning")
                        .param("meetingDate", DATE)
                        .param("startTime", "09:00")
                        .param("endTime", "10:00")
                        .param("roomNumber", String.valueOf(regularRoom)))
                .andExpect(redirectedUrl("/schedule"))
                .andExpect(flash().attribute("successMessage", is("Meeting \"Sprint Planning\" created.")));

        assertEquals(1, countMeetings());
        assertEquals(clientId, (int) jdbc.queryForObject(
                "SELECT userID FROM Meetings WHERE meetingName = ?", Integer.class, "Sprint Planning"));
        assertEquals("SCHEDULED", jdbc.queryForObject(
                "SELECT status FROM Meetings WHERE meetingName = ?", String.class, "Sprint Planning"));
    }

    @Test
    void rejectsMeetingOutsideBusinessHours() throws Exception {
        mockMvc.perform(post("/meetings/create").with(asClient()).with(csrf())
                        .param("meetingName", "Late Night")
                        .param("meetingDate", DATE)
                        .param("startTime", "16:00")
                        .param("endTime", "18:00")   // past the 17:00 business-hours cutoff
                        .param("roomNumber", String.valueOf(regularRoom)))
                .andExpect(status().isOk())
                .andExpect(view().name("create-meeting"))
                .andExpect(model().attributeExists("errorMessage"));

        assertEquals(0, countMeetings());
    }

    @Test
    void rejectsRoomDoubleBooking() throws Exception {
        seedMeeting("Existing", DATE, "09:00", "10:00", clientId, regularRoom);

        mockMvc.perform(post("/meetings/create").with(asClient()).with(csrf())
                        .param("meetingName", "Overlap")
                        .param("meetingDate", DATE)
                        .param("startTime", "09:00")
                        .param("endTime", "10:00")
                        .param("roomNumber", String.valueOf(regularRoom)))
                .andExpect(status().isOk())
                .andExpect(view().name("create-meeting"))
                .andExpect(model().attributeExists("errorMessage"));

        assertEquals(1, countMeetings());
    }

    @Test
    void rejectsCreatorDoubleBookingAcrossRooms() throws Exception {
        // Client already booked in the regular room at this slot...
        seedMeeting("Booked", DATE, "09:00", "10:00", clientId, regularRoom);

        // ...so a second meeting in a *different* room at the same time is refused.
        mockMvc.perform(post("/meetings/create").with(asClient()).with(csrf())
                        .param("meetingName", "Clash")
                        .param("meetingDate", DATE)
                        .param("startTime", "09:00")
                        .param("endTime", "10:00")
                        .param("roomNumber", String.valueOf(specialRoom)))
                .andExpect(status().isOk())
                .andExpect(view().name("create-meeting"))
                .andExpect(model().attributeExists("errorMessage"));

        assertEquals(1, countMeetings());
    }

    @Test
    void adjacentTimeSlotInSameRoomIsAllowed() throws Exception {
        seedMeeting("Morning", DATE, "09:00", "10:00", clientId, regularRoom);

        mockMvc.perform(post("/meetings/create").with(asClient()).with(csrf())
                        .param("meetingName", "Mid-Morning")
                        .param("meetingDate", DATE)
                        .param("startTime", "10:00")
                        .param("endTime", "11:00")
                        .param("roomNumber", String.valueOf(regularRoom)))
                .andExpect(redirectedUrl("/schedule"));

        assertEquals(2, countMeetings());
    }

    // ---- special-room payment confirmation gate ----

    @Test
    void specialRoomRequiresPaymentConfirmationBeforePersisting() throws Exception {
        mockMvc.perform(post("/meetings/create").with(asClient()).with(csrf())
                        .param("meetingName", "Board Meeting")
                        .param("meetingDate", DATE)
                        .param("startTime", "09:00")
                        .param("endTime", "10:00")
                        .param("roomNumber", String.valueOf(specialRoom)))
                .andExpect(status().isOk())
                .andExpect(view().name("confirm-payment"))
                .andExpect(model().attribute("fee", is(150.0)));

        assertEquals(0, countMeetings(), "special-room meeting must not be saved until payment is confirmed");
    }

    @Test
    void specialRoomIsBookedOncePaymentConfirmed() throws Exception {
        mockMvc.perform(post("/meetings/create").with(asClient()).with(csrf())
                        .param("meetingName", "Board Meeting")
                        .param("meetingDate", DATE)
                        .param("startTime", "09:00")
                        .param("endTime", "10:00")
                        .param("roomNumber", String.valueOf(specialRoom))
                        .param("confirmed", "true"))
                .andExpect(redirectedUrl("/schedule"));

        assertEquals(1, countMeetings());
    }

    // ---- listing ----

    @Test
    void scheduleListsTheClientsMeeting() throws Exception {
        seedMeeting("Retro", DATE, "11:00", "12:00", clientId, regularRoom);

        mockMvc.perform(get("/schedule").with(asClient()))
                .andExpect(status().isOk())
                .andExpect(view().name("schedule"))
                .andExpect(model().attribute("meetings",
                        hasItem(hasProperty("meetingName", is("Retro")))));
    }

    // ---- edit / delete authorization ----

    @Test
    void creatorCanEditOwnMeeting() throws Exception {
        int id = seedMeeting("Original", DATE, "09:00", "10:00", clientId, regularRoom);

        mockMvc.perform(post("/meetings/" + id + "/edit").with(asClient()).with(csrf())
                        .param("meetingName", "Renamed")
                        .param("meetingDate", DATE)
                        .param("startTime", "13:00")
                        .param("endTime", "14:00")
                        .param("roomNumber", String.valueOf(regularRoom)))
                .andExpect(redirectedUrl("/schedule"));

        assertEquals("Renamed", jdbc.queryForObject(
                "SELECT meetingName FROM Meetings WHERE meetingID = ?", String.class, id));
    }

    @Test
    void nonCreatorCannotEditMeeting() throws Exception {
        int id = seedMeeting("Owned", DATE, "09:00", "10:00", clientId, regularRoom);

        // The admin is authenticated but is not the meeting's creator.
        mockMvc.perform(post("/meetings/" + id + "/edit")
                        .with(user(ADMIN_EMAIL).roles("ADMINISTRATOR")).with(csrf())
                        .param("meetingName", "Hijacked")
                        .param("meetingDate", DATE)
                        .param("startTime", "13:00")
                        .param("endTime", "14:00")
                        .param("roomNumber", String.valueOf(regularRoom)))
                .andExpect(redirectedUrl("/schedule"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Unchanged.
        assertEquals("Owned", jdbc.queryForObject(
                "SELECT meetingName FROM Meetings WHERE meetingID = ?", String.class, id));
    }

    @Test
    void creatorCanDeleteOwnMeeting() throws Exception {
        int id = seedMeeting("Doomed", DATE, "09:00", "10:00", clientId, regularRoom);

        mockMvc.perform(post("/meetings/" + id + "/delete").with(asClient()).with(csrf()))
                .andExpect(redirectedUrl("/schedule"));

        assertEquals(0, countMeetings());
    }
}
