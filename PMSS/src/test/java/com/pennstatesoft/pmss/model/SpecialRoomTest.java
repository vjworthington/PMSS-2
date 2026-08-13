package com.pennstatesoft.pmss.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SpecialRoomTest {

    @Test
    void defaultFeeIsOneHundred() {
        SpecialRoom room = new SpecialRoom(300);

        assertEquals(300, room.getRoomNumber());
        assertEquals(100.00, room.getFee(), 0.0001);
    }

    @Test
    void setFeeUpdatesFee() {
        SpecialRoom room = new SpecialRoom(300);

        room.setFee(250.50);

        assertEquals(250.50, room.getFee(), 0.0001);
    }

    @Test
    void inheritsRoomBehaviour() {
        SpecialRoom room = new SpecialRoom(300);

        assertFalse(room.isOccupied());
        assertEquals(0, room.getMeetings().size());
    }
}
