package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Meeting;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class MeetingRowMapperTest {

    private final MeetingRowMapper mapper = new MeetingRowMapper();

    @Test
    void mapsAllColumns() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("meetingID")).thenReturn(12);
        when(rs.getString("meetingName")).thenReturn("Planning");
        when(rs.getString("meetingDate")).thenReturn("2026-07-04");
        when(rs.getInt("roomNumber")).thenReturn(3);
        when(rs.getString("startTime")).thenReturn("09:00:00");
        when(rs.getString("endTime")).thenReturn("10:00:00");
        when(rs.getInt("userID")).thenReturn(88);
        when(rs.getString("status")).thenReturn("SCHEDULED");

        Meeting meeting = mapper.mapRow(rs, 0);

        assertEquals(12, meeting.getMeetingID());
        assertEquals("Planning", meeting.getMeetingName());
        assertEquals(Date.valueOf("2026-07-04"), meeting.getMeetingDate());
        assertEquals(3, meeting.getRoomNumber());
        assertEquals(Time.valueOf("09:00:00"), meeting.getStartTime());
        assertEquals(Time.valueOf("10:00:00"), meeting.getEndTime());
        assertEquals(88, meeting.getCreatorID());
        assertEquals("SCHEDULED", meeting.getStatus());
    }

    @Test
    void padsShortTimeValues() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("meetingID")).thenReturn(1);
        when(rs.getString("meetingName")).thenReturn("M");
        when(rs.getString("meetingDate")).thenReturn("2026-07-04");
        when(rs.getInt("roomNumber")).thenReturn(1);
        when(rs.getString("startTime")).thenReturn("09:00");
        when(rs.getString("endTime")).thenReturn("17:00");
        when(rs.getInt("userID")).thenReturn(1);
        when(rs.getString("status")).thenReturn("SCHEDULED");

        Meeting meeting = mapper.mapRow(rs, 0);

        assertEquals(Time.valueOf("09:00:00"), meeting.getStartTime());
        assertEquals(Time.valueOf("17:00:00"), meeting.getEndTime());
    }

    @Test
    void handlesNullDateAndTimes() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("meetingID")).thenReturn(1);
        when(rs.getString("meetingName")).thenReturn("M");
        when(rs.getString("meetingDate")).thenReturn(null);
        when(rs.getInt("roomNumber")).thenReturn(1);
        when(rs.getString("startTime")).thenReturn(null);
        when(rs.getString("endTime")).thenReturn("   ");
        when(rs.getInt("userID")).thenReturn(1);
        when(rs.getString("status")).thenReturn("SCHEDULED");

        Meeting meeting = mapper.mapRow(rs, 0);

        assertNull(meeting.getMeetingDate());
        assertNull(meeting.getStartTime());
        assertNull(meeting.getEndTime());
    }
}
