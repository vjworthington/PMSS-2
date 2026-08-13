package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.Room;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.MeetingRowMapper;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserService userService = mock(UserService.class);
    private final MeetingRowMapper mapper = mock(MeetingRowMapper.class);
    private final ScheduleController controller = new ScheduleController(jdbc, userService, mapper);

    private final List<Meeting> result = List.of(new Meeting(1, "M", LocalDate.parse("2026-01-01"), 1));

    private User user(String role) {
        return new Client(7, "u@pennstatesoft.com", "h", "L", "F", role,
                "2000-01-01", "F L", null, 0, null, null);
    }

    private void stubQueryWithArgs() {
        when(jdbc.query(anyString(), eq(mapper), any(Object[].class))).thenReturn(result);
    }

    // ---- retrieval methods ----

    @Test
    void getCreatedMeetingsQueriesByUser() {
        stubQueryWithArgs();
        assertSame(result, controller.getCreatedMeetings(user("CLIENT")));
        verify(jdbc).query(anyString(), eq(mapper), eq(7));
    }

    @Test
    void getParticipatingMeetingsQueriesByUserTwice() {
        stubQueryWithArgs();
        assertSame(result, controller.getParticipatingMeetings(user("CLIENT")));
        verify(jdbc).query(anyString(), eq(mapper), eq(7), eq(7));
    }

    @Test
    void getUsersMeetingsQueriesByUserTwice() {
        stubQueryWithArgs();
        assertSame(result, controller.getUsersMeetings(user("CLIENT")));
        verify(jdbc).query(anyString(), eq(mapper), eq(7), eq(7));
    }

    @Test
    void getMeetingsByDayPassesDate() {
        stubQueryWithArgs();
        assertSame(result, controller.getMeetingsByDay(LocalDate.parse("2026-05-20")));
        verify(jdbc).query(anyString(), eq(mapper), eq("2026-05-20"));
    }

    @Test
    void getMeetingsByWeekExpandsToMondayThroughSunday() {
        stubQueryWithArgs();
        LocalDate input = LocalDate.parse("2026-04-15");
        LocalDate monday = input.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        controller.getMeetingsByWeek(LocalDate.parse(input.toString()));

        verify(jdbc).query(anyString(), eq(mapper), eq(monday.toString()), eq(sunday.toString()));
    }

    @Test
    void getMeetingsByRoomPassesRoomNumber() {
        stubQueryWithArgs();
        assertSame(result, controller.getMeetingsByRoom(new Room(42)));
        verify(jdbc).query(anyString(), eq(mapper), eq(42));
    }

    @Test
    void getMeetingsByPersonQueriesByUserTwice() {
        stubQueryWithArgs();
        assertSame(result, controller.getMeetingsByPerson(user("CLIENT")));
        verify(jdbc).query(anyString(), eq(mapper), eq(7), eq(7));
    }

    @Test
    void getMeetingsByTimeSlotPassesEndThenStart() {
        stubQueryWithArgs();
        controller.getMeetingsByTimeSlot(LocalTime.parse("09:00:00"), LocalTime.parse("10:00:00"));
        // SQL is "startTime < endParam AND endTime > startParam", so end is bound first.
        verify(jdbc).query(anyString(), eq(mapper), eq("10:00:00"), eq("09:00:00"));
    }

    // ---- displaySchedule dispatch ----

    @Test
    void displayScheduleRendersClientDashboard() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("u@pennstatesoft.com");
        when(userService.findByEmail("u@pennstatesoft.com")).thenReturn(user("CLIENT"));
        when(jdbc.query(anyString(), eq(mapper), any(Object[].class))).thenReturn(result);
        Model model = new ExtendedModelMap();

        String view = controller.displaySchedule(null, null, null, null, null, null, null, model, auth);

        assertEquals("schedule", view);
        assertEquals("all", model.getAttribute("view"));
        assertSame(result, model.getAttribute("meetings"));
    }

    @Test
    void displayScheduleRendersAdminDashboard() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@pennstatesoft.com");
        when(userService.findByEmail("admin@pennstatesoft.com")).thenReturn(user("ADMINISTRATOR"));
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(result); // findAllMeetings
        when(jdbc.queryForList(anyString())).thenReturn(List.of());             // rooms + users
        Model model = new ExtendedModelMap();

        String view = controller.displaySchedule(null, null, null, null, null, null, null, model, auth);

        assertEquals("schedule", view);
        assertEquals("all", model.getAttribute("filter"));
        assertSame(result, model.getAttribute("meetings"));
    }
}
