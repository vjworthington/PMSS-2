package com.pennstatesoft.pmss.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingTest {

    private static Client client(int userID) {
        return new Client(userID, "user" + userID + "@pennstatesoft.com", "hash",
                "Last", "First", "CLIENT", "2000-01-01", "Display",
                null, 0, null, null);
    }

    private static Meeting sampleMeeting() {
        return new Meeting(1, "Kickoff", LocalDate.parse("2026-05-01"), 10);
    }

    @Test
    void constructorInitializesCoreFields() {
        Meeting meeting = sampleMeeting();

        assertEquals(1, meeting.getMeetingID());
        assertEquals("Kickoff", meeting.getMeetingName());
        assertEquals(LocalDate.parse("2026-05-01"), meeting.getMeetingDate());
        assertEquals(10, meeting.getRoomNumber());
        assertTrue(meeting.getAttendees().isEmpty(), "attendee list starts empty");
    }

    @Test
    void settersRoundTrip() {
        Meeting meeting = sampleMeeting();

        meeting.setMeetingName("Renamed");
        meeting.setMeetingDate(LocalDate.parse("2026-06-02"));
        meeting.setStartTime(LocalTime.parse("09:00:00"));
        meeting.setEndTime(LocalTime.parse("10:00:00"));
        meeting.setCreatorID(55);
        meeting.setRoomNumber(20);
        meeting.setStatus("SCHEDULED");

        assertEquals("Renamed", meeting.getMeetingName());
        assertEquals(LocalDate.parse("2026-06-02"), meeting.getMeetingDate());
        assertEquals(LocalTime.parse("09:00:00"), meeting.getStartTime());
        assertEquals(LocalTime.parse("10:00:00"), meeting.getEndTime());
        assertEquals(55, meeting.getCreatorID());
        assertEquals(20, meeting.getRoomNumber());
        assertEquals("SCHEDULED", meeting.getStatus());
    }

    @Test
    void addAttendeeAddsNewClient() {
        Meeting meeting = sampleMeeting();

        assertTrue(meeting.addAttendee(client(1)));
        assertEquals(1, meeting.getAttendees().size());
    }

    @Test
    void addAttendeeRejectsNull() {
        Meeting meeting = sampleMeeting();

        assertFalse(meeting.addAttendee(null));
        assertTrue(meeting.getAttendees().isEmpty());
    }

    @Test
    void addAttendeeRejectsDuplicateReference() {
        Meeting meeting = sampleMeeting();
        Client c = client(1);

        assertTrue(meeting.addAttendee(c));
        assertFalse(meeting.addAttendee(c), "the same client cannot be added twice");
        assertEquals(1, meeting.getAttendees().size());
    }

    @Test
    void removeAttendeeRemovesMatchingUserId() {
        Meeting meeting = sampleMeeting();
        meeting.addAttendee(client(1));
        meeting.addAttendee(client(2));

        assertTrue(meeting.removeAttendee(1));
        assertEquals(1, meeting.getAttendees().size());
        assertEquals(2, meeting.getAttendees().get(0).getUserID());
    }

    @Test
    void removeAttendeeReturnsFalseWhenNotPresent() {
        Meeting meeting = sampleMeeting();
        meeting.addAttendee(client(1));

        assertFalse(meeting.removeAttendee(999));
        assertEquals(1, meeting.getAttendees().size());
    }

    @Test
    void getRoomIdDefaultsToZero() {
        // The constructor assigns roomId from itself before it is set, so it is always 0.
        assertEquals(0, sampleMeeting().getRoomId());
    }
}
