package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Meeting;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final MeetingRepository repository = new MeetingRepository(jdbc);

    private Meeting fullMeeting() {
        Meeting meeting = new Meeting(5, "Sync", Date.valueOf("2026-04-01"), 12);
        meeting.setStartTime(Time.valueOf("09:00:00"));
        meeting.setEndTime(Time.valueOf("10:00:00"));
        meeting.setCreatorID(3);
        meeting.setStatus("SCHEDULED");
        return meeting;
    }

    @Test
    void createMeetingPassesMappedFieldsAndReturnsRowCount() {
        when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        int result = repository.createMeeting(fullMeeting());

        assertEquals(1, result);
        verify(jdbc).update(anyString(),
                eq("Sync"), eq("2026-04-01"), eq("09:00:00"), eq("10:00:00"),
                eq(3), eq(12), eq("SCHEDULED"));
    }

    @Test
    void findByIdDelegatesToQueryForObject() {
        Meeting expected = fullMeeting();
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), any())).thenReturn(expected);

        assertSame(expected, repository.findById(5));
    }

    @Test
    void findByCreatorReturnsQueryResults() {
        List<Meeting> expected = List.of(fullMeeting());
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(expected);

        assertSame(expected, repository.findByCreator(3));
    }

    @Test
    void findAllReturnsQueryResults() {
        List<Meeting> expected = List.of(fullMeeting());
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(expected);

        assertSame(expected, repository.findAll());
    }

    @Test
    void updateMeetingReturnsRowCount() {
        when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        assertEquals(1, repository.updateMeeting(fullMeeting()));
    }

    @Test
    void deleteMeetingReturnsRowCount() {
        when(jdbc.update(anyString(), eq(9))).thenReturn(1);

        assertEquals(1, repository.deleteMeeting(9));
    }

    @Test
    void isRoomAvailableTrueWhenNoConflicts() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenReturn(0);

        assertTrue(repository.isRoomAvailable(1, Time.valueOf("09:00:00"), Time.valueOf("10:00:00")));
    }

    @Test
    void isRoomAvailableFalseWhenConflicts() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenReturn(2);

        assertFalse(repository.isRoomAvailable(1, Time.valueOf("09:00:00"), Time.valueOf("10:00:00")));
    }

    @Test
    void isRoomAvailableFalseWhenCountNull() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenReturn(null);

        assertFalse(repository.isRoomAvailable(1, Time.valueOf("09:00:00"), Time.valueOf("10:00:00")));
    }
}
