package com.pennstatesoft.pmss.service;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.repository.MeetingRepository;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingServiceTest {

    private final MeetingRepository repository = mock(MeetingRepository.class);
    private final MeetingService service = new MeetingService(repository);

    private Meeting meeting() {
        Meeting m = new Meeting(1, "Sync", Date.valueOf("2026-04-01"), 12);
        m.setStartTime(Time.valueOf("09:00:00"));
        m.setEndTime(Time.valueOf("10:00:00"));
        return m;
    }

    @Test
    void createMeetingReturnsFalseForNull() {
        assertFalse(service.createMeeting(null));
        verify(repository, never()).createMeeting(any());
    }

    @Test
    void createMeetingTrueWhenRowInserted() {
        when(repository.createMeeting(any())).thenReturn(1);
        assertTrue(service.createMeeting(meeting()));
    }

    @Test
    void createMeetingFalseWhenNoRowInserted() {
        when(repository.createMeeting(any())).thenReturn(0);
        assertFalse(service.createMeeting(meeting()));
    }

    @Test
    void getMeetingDelegatesToRepository() {
        Meeting expected = meeting();
        when(repository.findById(1)).thenReturn(expected);
        assertSame(expected, service.getMeeting(1));
    }

    @Test
    void getAllMeetingsDelegatesToRepository() {
        List<Meeting> expected = List.of(meeting());
        when(repository.findAll()).thenReturn(expected);
        assertSame(expected, service.getAllMeetings());
    }

    @Test
    void getUserMeetingsDelegatesToRepository() {
        List<Meeting> expected = List.of(meeting());
        when(repository.findByCreator(7)).thenReturn(expected);
        assertSame(expected, service.getUserMeetings(7));
    }

    @Test
    void updateMeetingReturnsFalseForNull() {
        assertFalse(service.updateMeeting(null));
        verify(repository, never()).updateMeeting(any());
    }

    @Test
    void updateMeetingTrueWhenRowUpdated() {
        when(repository.updateMeeting(any())).thenReturn(1);
        assertTrue(service.updateMeeting(meeting()));
    }

    @Test
    void deleteMeetingTrueWhenRowDeleted() {
        when(repository.deleteMeeting(4)).thenReturn(1);
        assertTrue(service.deleteMeeting(4));
    }

    @Test
    void deleteMeetingFalseWhenNothingDeleted() {
        when(repository.deleteMeeting(4)).thenReturn(0);
        assertFalse(service.deleteMeeting(4));
    }

    @Test
    void scheduleMeetingThrowsWhenRoomUnavailable() {
        when(repository.isRoomAvailable(anyInt(), any(), any())).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.scheduleMeeting(meeting()));
        verify(repository, never()).createMeeting(any());
    }

    @Test
    void scheduleMeetingCreatesWhenRoomAvailable() {
        when(repository.isRoomAvailable(anyInt(), any(), any())).thenReturn(true);

        service.scheduleMeeting(meeting());

        verify(repository).createMeeting(any());
    }
}
