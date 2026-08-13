package com.pennstatesoft.pmss.model;

import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomTest {

    @Test
    void newRoomIsUnoccupiedWithNoMeetings() {
        Room room = new Room(101);

        assertEquals(101, room.getRoomNumber());
        assertFalse(room.isOccupied());
        assertTrue(room.getMeetings().isEmpty());
    }

    @Test
    void setRoomNumberUpdatesNumber() {
        Room room = new Room(1);

        room.setRoomNumber(205);

        assertEquals(205, room.getRoomNumber());
    }

    @Test
    void setOccupiedTogglesFlag() {
        Room room = new Room(1);

        room.setOccupied(true);
        assertTrue(room.isOccupied());

        room.setOccupied(false);
        assertFalse(room.isOccupied());
    }

    @Test
    void addMeetingStoresMeeting() {
        Room room = new Room(1);
        Meeting meeting = new Meeting(1, "Standup", Date.valueOf("2026-01-01"), 1);

        room.addMeeting(meeting);

        assertEquals(1, room.getMeetings().size());
        assertEquals(meeting, room.getMeetings().get(0));
    }
}
