package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.MeetingService;
import com.pennstatesoft.pmss.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/meetings")
public class  MeetingController {

    private final MeetingService meetingService;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityLogger securityLogger;
    private static final String MEETING_URL = "redirect:/meetings/";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String MEETING_NAME = "meetingName";
    private static final String MEETING_PATH = "Meeting \"";
    private static final String ROOM_NUMBER = "roomNumber";
    private static final String MEETING_DATE = "meetingDate";
    private static final String START_TIME = "startTime";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SCHEDULE_URL = "redirect:/schedule";
    private static final String END_TIME = "endTime";
    private static final String EDIT_URL = "/edit";


    public MeetingController(MeetingService meetingService,
                             UserService userService,
                             JdbcTemplate jdbcTemplate,
                             SecurityLogger securityLogger) {
        this.meetingService = meetingService;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.securityLogger = securityLogger;
    }

    // The meetings list is consolidated into the Schedule page; /meetings redirects there.
    @GetMapping
    public String displayMeetings() {
        return SCHEDULE_URL;
    }

    // MeetingEditDashboard (create mode): OpenCreateMeetingForm.
    @GetMapping("/create")
    public String showCreateMeetingPage(Model model, Authentication authentication) {
        model.addAttribute("user", userService.findByEmail(authentication.getName()));
        addFormOptions(model);
        return "create-meeting";
    }

    // MeetingEditDashboard SubmitMeeting: validate details + room availability, then create.
    @PostMapping("/create")
    public String createMeeting(@RequestParam(name = "meetingName", required = false) String meetingName,
                                @RequestParam(name = "meetingDate", required = false) String meetingDate,
                                @RequestParam(name = "startTime", required = false) String startTime,
                                @RequestParam(name = "endTime", required = false) String endTime,
                                @RequestParam(name = "roomNumber", required = false) Integer roomNumber,
                                @RequestParam(name = "confirmed", required = false, defaultValue = "false") boolean confirmed,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        User user = userService.findByEmail(authentication.getName());

        // ValidateMeetingDetails
        String error = validateMeeting(meetingName, meetingDate, startTime, endTime, roomNumber);

        // CheckRoomAvailability
        if (error == null && !roomIsAvailable(roomNumber, meetingDate, startTime + ":00", endTime + ":00", 0)) {
            error = "Room " + roomNumber + " is already booked during that time slot.";
        }

        // The creator cannot be double-booked in the same slot.
        if (error == null && !clientIsAvailable(user.getUserID(), meetingDate, startTime + ":00", endTime + ":00", 0)) {
            error = "You already have a meeting during that time slot.";
        }

        if (error != null) {
            model.addAttribute("user", user);
            model.addAttribute(ERROR_MESSAGE, error);
            model.addAttribute(MEETING_NAME, meetingName);
            model.addAttribute(MEETING_DATE, meetingDate);
            model.addAttribute(START_TIME, startTime);
            model.addAttribute(END_TIME, endTime);
            model.addAttribute(ROOM_NUMBER, roomNumber);
            addFormOptions(model);
            return "create-meeting";
        }

        // Show a simple "pay the fee to reserve?" confirmation.
        Map<String, Object> room = findRoom(roomNumber);
        double fee = (room == null || room.get("fee") == null)
                ? 0.0 : ((Number) room.get("fee")).doubleValue();
        boolean special = room != null && "SPECIAL".equals(room.get("roomType"));

        if (special && fee > 0 && !confirmed) {
            model.addAttribute("user", user);
            model.addAttribute(MEETING_NAME, meetingName);
            model.addAttribute(MEETING_DATE, meetingDate);
            model.addAttribute(START_TIME, startTime);
            model.addAttribute(END_TIME, endTime);
            model.addAttribute(ROOM_NUMBER, roomNumber);
            model.addAttribute("fee", fee);
            return "confirm-payment";
        }

        Meeting meeting = new Meeting(
                0,
                meetingName.trim(),
                LocalDate.parse(meetingDate),
                roomNumber
        );
        meeting.setCreatorID(user.getUserID());
        meeting.setStartTime(LocalTime.parse(startTime));
        meeting.setEndTime(LocalTime.parse(endTime));
        meeting.setStatus("SCHEDULED");

        meetingService.createMeeting(meeting);
        securityLogger.meetingCreated(authentication.getName(), meeting.getMeetingName());

        String successMessage = (special && fee > 0)
                ? MEETING_PATH + meeting.getMeetingName() + "\" reserved — $"
                        + String.format("%.2f", fee) + " paid for Room " + roomNumber + "."
                : MEETING_PATH + meeting.getMeetingName() + "\" created.";
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, successMessage);
        return SCHEDULE_URL;
    }

    // MeetingEditDashboard (edit mode): OpenEditMeetingForm.
    @GetMapping("/{id}/edit")
    public String showEditMeetingPage(@PathVariable("id") int id,
                                      Model model,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());
        Meeting meeting = meetingService.getMeeting(id);

        String denied = requireCreator(meeting, user, redirectAttributes);
        if (denied != null) {
            return denied;
        }

        model.addAttribute("user", user);
        populateEditModel(model, meeting);
        return "edit-meeting";
    }

    // MeetingEditDashboard SubmitChanges: validate + update (creator only).
    @PostMapping("/{id}/edit")
    public String updateMeeting(@PathVariable("id") int id,
                                @RequestParam(name = "meetingName", required = false) String meetingName,
                                @RequestParam(name = "meetingDate", required = false) String meetingDate,
                                @RequestParam(name = "startTime", required = false) String startTime,
                                @RequestParam(name = "endTime", required = false) String endTime,
                                @RequestParam(name = "roomNumber", required = false) Integer roomNumber,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());
        Meeting meeting = meetingService.getMeeting(id);

        String denied = requireCreator(meeting, user, redirectAttributes);
        if (denied != null) {
            return denied;
        }

        String error = validateMeeting(meetingName, meetingDate, startTime, endTime, roomNumber);
        if (error == null && !roomIsAvailable(roomNumber, meetingDate, startTime + ":00", endTime + ":00", id)) {
            error = "Room " + roomNumber + " is already booked during that time slot.";
        }

        // CheckClientAvailability: the creator cannot be double-booked in the same slot.
        if (error == null && !clientIsAvailable(user.getUserID(), meetingDate, startTime + ":00", endTime + ":00", id)) {
            error = "You already have a meeting during that time slot.";
        }

        if (error != null) {
            model.addAttribute("user", user);
            model.addAttribute(ERROR_MESSAGE, error);
            model.addAttribute("meetingId", id);
            model.addAttribute(MEETING_NAME, meetingName);
            model.addAttribute(MEETING_DATE, meetingDate);
            model.addAttribute(START_TIME, startTime);
            model.addAttribute(END_TIME, endTime);
            model.addAttribute(ROOM_NUMBER, roomNumber);
            addFormOptions(model);
            model.addAttribute("attendees", findAttendees(id));
            model.addAttribute("addableClients", findAddableClients(id, meeting.getCreatorID()));
            return "edit-meeting";
        }

        meeting.setMeetingName(meetingName.trim());
        meeting.setMeetingDate(LocalDate.parse(meetingDate));        meeting.setStartTime(LocalTime.parse(startTime + ":00"));
        meeting.setEndTime(LocalTime.parse(endTime + ":00"));
        meeting.setRoomNumber(roomNumber);
        if (meeting.getStatus() == null) {
            meeting.setStatus("SCHEDULED");
        }

        meetingService.updateMeeting(meeting);

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                MEETING_PATH + meeting.getMeetingName() + "\" updated.");
        return SCHEDULE_URL;
    }

    // MeetingController DeleteMeeting: creator-only remove + release room.
    @PostMapping("/{id}/delete")
    public String deleteMeeting(@PathVariable("id") int id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());
        Meeting meeting = meetingService.getMeeting(id);

        String denied = requireCreator(meeting, user, redirectAttributes);
        if (denied != null) {
            return denied;
        }

        jdbcTemplate.update("DELETE FROM MeetingAttendees WHERE meetingID = ?", id);
        meetingService.deleteMeeting(id);
        securityLogger.meetingDeleted(authentication.getName(), id);

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                MEETING_PATH + meeting.getMeetingName() + "\" deleted.");
        return SCHEDULE_URL;
    }

    // MeetingEditDashboard AddParticipant: creator only, checks the client is free.
    @PostMapping("/{id}/attendees/add")
    public String addParticipant(@PathVariable("id") int id,
                                 @RequestParam(name = "userID", required = false) Integer participantID,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());
        Meeting meeting = meetingService.getMeeting(id);

        String denied = requireCreator(meeting, user, redirectAttributes);
        if (denied != null) {
            return denied;
        }

        if (participantID == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please choose a person to add.");
            return MEETING_URL + id + EDIT_URL;
        }

        String date = meeting.getMeetingDate().toString();
        String start = meeting.getStartTime().toString();
        String end = meeting.getEndTime().toString();

        if (!clientIsAvailable(participantID, date, start, end, id)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                    "That person already has a meeting during this time slot.");
            return MEETING_URL + id + EDIT_URL;
        }

        jdbcTemplate.update(
                "INSERT OR IGNORE INTO MeetingAttendees (meetingID, userID) VALUES (?, ?)",
                id, participantID);
        securityLogger.attendeeAdded(authentication.getName(), id, participantID);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Participant added.");
        return MEETING_URL + id + EDIT_URL;
    }

    // MeetingEditDashboard RemoveParticipant: creator only.
    @PostMapping("/{id}/attendees/remove")
    public String removeParticipant(@PathVariable("id") int id,
                                    @RequestParam(name = "userID", required = false) Integer participantID,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());
        Meeting meeting = meetingService.getMeeting(id);

        String denied = requireCreator(meeting, user, redirectAttributes);
        if (denied != null) {
            return denied;
        }

        jdbcTemplate.update(
                "DELETE FROM MeetingAttendees WHERE meetingID = ? AND userID = ?",
                id, participantID);
        securityLogger.attendeeRemoved(authentication.getName(), id, participantID);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Participant removed.");
        return MEETING_URL + id + EDIT_URL;
    }

    // --- helpers -------------------------------------------------------------

    private void addFormOptions(Model model) {
        model.addAttribute("rooms", jdbcTemplate.queryForList(
                "SELECT roomNumber, roomType, fee FROM Rooms ORDER BY roomNumber"));
        model.addAttribute("timeSlots", List.of(
                "09:00", "10:00", "11:00", "12:00", "13:00",
                "14:00", "15:00", "16:00", "17:00"));
    }

    private String validateMeeting(String name, String date, String start, String end, Integer roomNumber) {
        if (name == null || name.isBlank()) {
            return "Meeting name is required.";
        }
        if (date == null || date.isBlank()) {
            return "Meeting date is required.";
        }
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            return "Start and end time are required.";
        }
        if (roomNumber == null) {
            return "Please select a room.";
        }
        // Business hours: 09:00–17:00
        if (start.compareTo("09:00") < 0 || end.compareTo("17:00") > 0) {
            return "Meetings must be within business hours (09:00–17:00).";
        }
        if (end.compareTo(start) <= 0) {
            return "End time must be after the start time.";
        }
        try {
            java.sql.Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            return "Enter a valid meeting date.";
        }
        return null;
    }

    private boolean roomIsAvailable(int roomNumber, String date, String startFull, String endFull,
                                    int excludeMeetingID) {
        Integer conflicts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Meetings WHERE roomNumber = ? AND meetingDate = ? "
                        + "AND startTime < ? AND endTime > ? AND meetingID <> ?",
                Integer.class, roomNumber, date, endFull, startFull, excludeMeetingID);
        return conflicts == null || conflicts == 0;
    }

    private Map<String, Object> findRoom(int roomNumber) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT roomNumber, roomType, fee FROM Rooms WHERE roomNumber = ?", roomNumber);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // Returns a redirect view string if the meeting is missing or the user is not its
    // creator
    private String requireCreator(Meeting meeting, User user, RedirectAttributes redirectAttributes) {
        if (meeting == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Meeting not found.");
            return SCHEDULE_URL;
        }
        if (meeting.getCreatorID() != user.getUserID()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                    "Only the meeting's creator can manage it.");
            return SCHEDULE_URL;
        }
        return null;
    }

    private void populateEditModel(Model model, Meeting meeting) {
        model.addAttribute("meetingId", meeting.getMeetingID());
        model.addAttribute(MEETING_NAME, meeting.getMeetingName());
        model.addAttribute(MEETING_DATE, meeting.getMeetingDate().toString());
        model.addAttribute(START_TIME, meeting.getStartTime().toString().substring(0, 5));
        model.addAttribute(END_TIME, meeting.getEndTime().toString().substring(0, 5));
        model.addAttribute(ROOM_NUMBER, meeting.getRoomNumber());
        addFormOptions(model);
        model.addAttribute("attendees", findAttendees(meeting.getMeetingID()));
        model.addAttribute("addableClients",
                findAddableClients(meeting.getMeetingID(), meeting.getCreatorID()));
    }

    private List<Map<String, Object>> findAttendees(int meetingID) {
        return jdbcTemplate.queryForList("""
                SELECT u.userID, u.firstName, u.lastName
                FROM MeetingAttendees a
                JOIN Users u ON a.userID = u.userID
                WHERE a.meetingID = ?
                ORDER BY u.lastName, u.firstName
                """, meetingID);
    }

    private List<Map<String, Object>> findAddableClients(int meetingID, int creatorID) {
        return jdbcTemplate.queryForList("""
                SELECT userID, firstName, lastName
                FROM Users
                WHERE userID <> ?
                  AND userID NOT IN (SELECT userID FROM MeetingAttendees WHERE meetingID = ?)
                ORDER BY lastName, firstName
                """, creatorID, meetingID);
    }

    // CheckClientAvailability: is the person free (no created/attended meeting overlapping)?
    private boolean clientIsAvailable(int userID, String date, String startFull, String endFull,
                                      int excludeMeetingID) {
        Integer conflicts = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM Meetings m
                LEFT JOIN MeetingAttendees a ON m.meetingID = a.meetingID
                WHERE (m.userID = ? OR a.userID = ?)
                  AND m.meetingID <> ?
                  AND m.meetingDate = ?
                  AND m.startTime < ? AND m.endTime > ?
                """, Integer.class, userID, userID, excludeMeetingID, date, endFull, startFull);
        return conflicts == null || conflicts == 0;
    }
}
