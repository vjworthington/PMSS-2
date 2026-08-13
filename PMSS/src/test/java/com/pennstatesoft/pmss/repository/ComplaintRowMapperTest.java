package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Complaint;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComplaintRowMapperTest {

    private final ComplaintRowMapper mapper = new ComplaintRowMapper();

    @Test
    void mapsAllColumnsIncludingResponse() throws SQLException {
        // The app stores dateFiled via SQLite datetime('now'): "yyyy-MM-dd HH:mm:ss".
        String filed = "2026-03-01 12:00:00";
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("userID")).thenReturn(4);
        when(rs.getInt("meetingID")).thenReturn(7);
        when(rs.getString("complaintOption")).thenReturn("Billing");
        when(rs.getString("summary")).thenReturn("Overcharged");
        when(rs.getInt("complaintID")).thenReturn(21);
        when(rs.getString("status")).thenReturn("RESOLVED");
        when(rs.getString("dateFiled")).thenReturn(filed);
        when(rs.getString("adminResponse")).thenReturn("Refunded");

        Complaint complaint = mapper.mapRow(rs, 0);

        assertEquals(4, complaint.getUserID());
        assertEquals(7, complaint.getMeetingID());
        assertEquals("Billing", complaint.getComplaintOption());
        assertEquals("Overcharged", complaint.getSummary());
        assertEquals(21, complaint.getComplaintID());
        assertEquals("RESOLVED", complaint.getStatus());
        assertEquals(LocalDate.of(2026, 3, 1), complaint.getDateFiled());
        assertEquals("Refunded", complaint.getAdminResponse());
    }

    @Test
    void restoringResponseKeepsStoredStatus() throws SQLException {
        // restoreAdminResponse must not flip status to RESOLVED the way setAdminResponse does.
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("userID")).thenReturn(1);
        when(rs.getInt("meetingID")).thenReturn(0);
        when(rs.getString("complaintOption")).thenReturn("Other");
        when(rs.getString("summary")).thenReturn("text");
        when(rs.getInt("complaintID")).thenReturn(1);
        when(rs.getString("status")).thenReturn("PENDING");
        when(rs.getTimestamp("dateFiled")).thenReturn(null);
        when(rs.getString("adminResponse")).thenReturn("draft note");

        Complaint complaint = mapper.mapRow(rs, 0);

        assertEquals("PENDING", complaint.getStatus());
        assertEquals("draft note", complaint.getAdminResponse());
    }

    @Test
    void nullResponseLeavesAdminResponseNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("userID")).thenReturn(1);
        when(rs.getInt("meetingID")).thenReturn(0);
        when(rs.getString("complaintOption")).thenReturn("Other");
        when(rs.getString("summary")).thenReturn("text");
        when(rs.getInt("complaintID")).thenReturn(1);
        when(rs.getString("status")).thenReturn("PENDING");
        when(rs.getTimestamp("dateFiled")).thenReturn(null);
        when(rs.getString("adminResponse")).thenReturn(null);

        Complaint complaint = mapper.mapRow(rs, 0);

        assertNull(complaint.getAdminResponse());
    }
}
