package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Adding and removing meeting participants (/meetings/{id}/attendees/...):
 * creator-only authorization, the double-booking check on the person being
 * added, and the resulting MeetingAttendees rows.
 */
class AttendeeManagementIT extends AbstractIntegrationTest {

    private static final String DATE = "2026-09-01";

    private int otherClientId;

    @Override
    protected void seedExtra() {
        otherClientId = seedUser("other@pennstatesoft.com", "CLIENT", "Olive", "Other");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asClient() {
        return user(CLIENT_EMAIL).roles("CLIENT");
    }

    private boolean isAttendee(int meetingId, int userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM MeetingAttendees WHERE meetingID = ? AND userID = ?",
                Integer.class, meetingId, userId) > 0;
    }

    // ---- add ----

    @Test
    void creatorAddsAvailableParticipant() throws Exception {
        int id = seedMeeting("Sync", DATE, "09:00", "10:00", clientId, regularRoom);

        mockMvc.perform(post("/meetings/" + id + "/attendees/add").with(asClient()).with(csrf())
                        .param("userID", String.valueOf(otherClientId)))
                .andExpect(redirectedUrl("/meetings/" + id + "/edit"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(true, isAttendee(id, otherClientId));
    }

    @Test
    void addingParticipantWithConflictingMeetingIsRejected() throws Exception {
        int id = seedMeeting("Sync", DATE, "09:00", "10:00", clientId, regularRoom);
        // The other client already runs their own meeting in the same slot.
        seedMeeting("Other's Own", DATE, "09:00", "10:00", otherClientId, specialRoom);

        mockMvc.perform(post("/meetings/" + id + "/attendees/add").with(asClient()).with(csrf())
                        .param("userID", String.valueOf(otherClientId)))
                .andExpect(redirectedUrl("/meetings/" + id + "/edit"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(false, isAttendee(id, otherClientId));
    }

    @Test
    void addingWithoutSelectingParticipantShowsError() throws Exception {
        int id = seedMeeting("Sync", DATE, "09:00", "10:00", clientId, regularRoom);

        mockMvc.perform(post("/meetings/" + id + "/attendees/add").with(asClient()).with(csrf()))
                .andExpect(redirectedUrl("/meetings/" + id + "/edit"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM MeetingAttendees WHERE meetingID = ?", Integer.class, id));
    }

    @Test
    void nonCreatorCannotAddParticipant() throws Exception {
        int id = seedMeeting("Sync", DATE, "09:00", "10:00", clientId, regularRoom);

        // Admin is authenticated but is not the meeting's creator.
        mockMvc.perform(post("/meetings/" + id + "/attendees/add")
                        .with(user(ADMIN_EMAIL).roles("ADMINISTRATOR")).with(csrf())
                        .param("userID", String.valueOf(otherClientId)))
                .andExpect(redirectedUrl("/schedule"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(false, isAttendee(id, otherClientId));
    }

    // ---- remove ----

    @Test
    void creatorRemovesParticipant() throws Exception {
        int id = seedMeeting("Sync", DATE, "09:00", "10:00", clientId, regularRoom);
        seedAttendee(id, otherClientId);

        mockMvc.perform(post("/meetings/" + id + "/attendees/remove").with(asClient()).with(csrf())
                        .param("userID", String.valueOf(otherClientId)))
                .andExpect(redirectedUrl("/meetings/" + id + "/edit"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(false, isAttendee(id, otherClientId));
    }

    @Test
    void nonCreatorCannotRemoveParticipant() throws Exception {
        int id = seedMeeting("Sync", DATE, "09:00", "10:00", clientId, regularRoom);
        seedAttendee(id, otherClientId);

        mockMvc.perform(post("/meetings/" + id + "/attendees/remove")
                        .with(user(ADMIN_EMAIL).roles("ADMINISTRATOR")).with(csrf())
                        .param("userID", String.valueOf(otherClientId)))
                .andExpect(redirectedUrl("/schedule"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Still an attendee — the non-creator's removal was refused.
        assertEquals(true, isAttendee(id, otherClientId));
    }
}
