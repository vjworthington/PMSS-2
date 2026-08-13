package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.Room;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.MeetingRowMapper;
import com.pennstatesoft.pmss.repository.UserRowMapper;
import com.pennstatesoft.pmss.service.UserService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/schedule")
public class ScheduleController implements AdminScheduleControllerIF {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final MeetingRowMapper meetingRowMapper;

    // Cached list of the meetings most recently retrieved for display
    private List<Meeting> meetings;

    public ScheduleController(JdbcTemplate jdbcTemplate,
                              UserService userService,
                              MeetingRowMapper meetingRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.meetingRowMapper = meetingRowMapper;
    }

    @GetMapping
    public String displaySchedule(@RequestParam(name = "view", required = false) String view,
                                  @RequestParam(name = "filter", required = false) String filter,
                                  @RequestParam(name = "day", required = false) String day,
                                  @RequestParam(name = "weekStart", required = false) String weekStart,
                                  @RequestParam(name = "roomNumber", required = false) Integer roomNumber,
                                  @RequestParam(name = "personID", required = false) Integer personID,
                                  @RequestParam(name = "timeSlot", required = false) String timeSlot,
                                  Model model,
                                  Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        if (user.getRole().equals("ADMINISTRATOR")) {
            return renderAdminDashboard(model, user, filter, day, weekStart, roomNumber, personID, timeSlot);
        }

        return renderClientDashboard(model, user, view);
    }

    // Client Dashboard
    private String renderClientDashboard(Model model, User user, String view) {
        String selectedView = (view == null || view.isBlank()) ? "all" : view;

        List<Meeting> displayed = switch (selectedView) {
            case "created" -> getCreatedMeetings(user);
            case "participating" -> getParticipatingMeetings(user);
            default -> getUsersMeetings(user);
        };

        model.addAttribute("view", selectedView);
        model.addAttribute("meetings", displayed);
        return "schedule";
    }

    // Admin Dashboard
    private String renderAdminDashboard(Model model,
                                        User user,
                                        String filter,
                                        String day,
                                        String weekStart,
                                        Integer roomNumber,
                                        Integer personID,
                                        String timeSlot) {

        String selectedFilter = (filter == null || filter.isBlank()) ? "all" : filter;
        List<Meeting> displayed;

        switch (selectedFilter) {
            case "day":
                displayed = List.of();
                if (day != null && !day.isBlank()) {
                    displayed = getMeetingsByDay(LocalDate.parse(day));
                }
                break;
            case "week":
                displayed = List.of();
                if (weekStart != null && !weekStart.isBlank()) {
                    displayed = getMeetingsByWeek(LocalDate.parse(weekStart));
                }
                break;
            case "room":
                displayed = List.of();
                if (roomNumber != null) {
                    displayed = getMeetingsByRoom(new Room(roomNumber));
                }
                break;
            case "person":
                User person = null;
                displayed = List.of();

                if (personID != null) {
                    person = findUserById(personID);
                }
                if (person != null) {
                    displayed = getMeetingsByPerson(person);
                }

                break;
            case "timeslot":
                displayed = List.of();
                if (timeSlot != null && !timeSlot.isBlank()) {
                    LocalTime start = normalizeTime(timeSlot);
                    LocalTime end = plusOneHour(start);
                    displayed = getMeetingsByTimeSlot(start, end);
                }
                break;
            case "created":
                displayed = getCreatedMeetings(user);
                break;
            case "participating":
                displayed = getParticipatingMeetings(user);
                break;
            default:
                displayed = findAllMeetings();
                break;
        }

        model.addAttribute("filter", selectedFilter);
        model.addAttribute("day", day);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("roomNumber", roomNumber);
        model.addAttribute("personID", personID);
        model.addAttribute("timeSlot", timeSlot);
        model.addAttribute("rooms", findAllRooms());
        model.addAttribute("people", findAllUsers());
        model.addAttribute("timeSlots", businessHourSlots());
        model.addAttribute("meetings", displayed);

        return "schedule";
    }

    @Override
    public List<Meeting> getCreatedMeetings(User user) {
        String sql = """
            SELECT * FROM Meetings
            WHERE userID = ?
            ORDER BY meetingDate, startTime
        """;
        meetings = jdbcTemplate.query(sql, meetingRowMapper, user.getUserID());
        return meetings;
    }

    @Override
    public List<Meeting> getParticipatingMeetings(User user) {
        String sql = """
            SELECT m.* FROM Meetings m
            JOIN MeetingAttendees a ON m.meetingID = a.meetingID
            WHERE a.userID = ? AND m.userID <> ?
            ORDER BY m.meetingDate, m.startTime
        """;
        meetings = jdbcTemplate.query(sql, meetingRowMapper, user.getUserID(), user.getUserID());
        return meetings;
    }

    @Override
    public List<Meeting> getUsersMeetings(User user) {
        String sql = """
            SELECT DISTINCT m.* FROM Meetings m
            LEFT JOIN MeetingAttendees a ON m.meetingID = a.meetingID
            WHERE m.userID = ? OR a.userID = ?
            ORDER BY m.meetingDate, m.startTime
        """;
        meetings = jdbcTemplate.query(sql, meetingRowMapper, user.getUserID(), user.getUserID());
        return meetings;
    }

    @Override
    public List<Meeting> getMeetingsByDay(LocalDate date) {
        String sql = """
            SELECT * FROM Meetings
            WHERE meetingDate = ?
            ORDER BY startTime
        """;
        meetings = jdbcTemplate.query(sql, meetingRowMapper, date.toString());
        return meetings;
    }

    @Override
    public List<Meeting> getMeetingsByWeek(LocalDate weekStart) {
        LocalDate dateInWeek = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate start = dateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6); // Sunday of the same week
        String sql = """
            SELECT * FROM Meetings
            WHERE meetingDate BETWEEN ? AND ?
            ORDER BY meetingDate, startTime
        """;
        meetings = jdbcTemplate.query(sql, meetingRowMapper, start.toString(), end.toString());
        return meetings;
    }

    @Override
    public List<Meeting> getMeetingsByRoom(Room room) {
        String sql = """
            SELECT * FROM Meetings
            WHERE roomNumber = ?
            ORDER BY meetingDate, startTime
        """;
        meetings = jdbcTemplate.query(sql, meetingRowMapper, room.getRoomNumber());
        return meetings;
    }

    @Override
    public List<Meeting> getMeetingsByPerson(User user) {
        return getUsersMeetings(user);
    }

    @Override
    public List<Meeting> getMeetingsByTimeSlot(LocalTime startTime, LocalTime endTime) {
        String sql = """
            SELECT * FROM Meetings
            WHERE startTime < ? AND endTime > ?
            ORDER BY meetingDate, startTime
        """;
        meetings = jdbcTemplate.query(sql, meetingRowMapper, endTime.toString(), startTime.toString());
        return meetings;
    }

    // Helper Methods

    private List<Meeting> findAllMeetings() {
        String sql = "SELECT * FROM Meetings ORDER BY meetingDate, startTime";
        meetings = jdbcTemplate.query(sql, meetingRowMapper);
        return meetings;
    }

    private User findUserById(int userID) {
        String sql = "SELECT * FROM Users WHERE userID = ?";
        List<User> results = jdbcTemplate.query(sql, new UserRowMapper(), userID);
        return results.isEmpty() ? null : results.get(0);
    }

    private List<Map<String, Object>> findAllRooms() {
        return jdbcTemplate.queryForList(
                "SELECT roomNumber, roomType FROM Rooms ORDER BY roomNumber");
    }

    private List<Map<String, Object>> findAllUsers() {
        return jdbcTemplate.queryForList("""
            SELECT userID, firstName, lastName, displayName
            FROM Users
            ORDER BY lastName, firstName
        """);
    }

    // Ordered map of each slot's start time to its "start-end" display label.
    private Map<String, String> businessHourSlots() {
        Map<String, String> slots = new LinkedHashMap<>();
        for (int hour = 9; hour <= 16; hour++) {
            String start = String.format("%02d:00", hour);
            String end = String.format("%02d:00", hour + 1);
            slots.put(start, start + "-" + end);
        }
        return slots;
    }

    private LocalTime normalizeTime(String value) {
        String trimmed = value.trim();
        if (trimmed.length() == 5) {
            trimmed = trimmed + ":00";
        }
        return LocalTime.parse(trimmed);
    }

    private LocalTime plusOneHour(LocalTime time) {
        return time.plusHours(1);
    }
}
