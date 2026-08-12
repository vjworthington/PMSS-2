package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.service.MeetingService;
import com.pennstatesoft.pmss.service.UserService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Time;
import java.util.List;

@Controller
@RequestMapping("/meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    public MeetingController(MeetingService meetingService,
                             UserService userService,
                             JdbcTemplate jdbcTemplate) {
        this.meetingService = meetingService;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // MeetingDashboard: displays the list of meetings.
    @GetMapping
    public String displayMeetings(Model model, Authentication authentication) {
        model.addAttribute("user", userService.findByEmail(authentication.getName()));
        model.addAttribute("meetings", meetingService.getAllMeetings());
        return "meetings";
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
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        User user = userService.findByEmail(authentication.getName());

        // ValidateMeetingDetails
        String error = validateMeeting(meetingName, meetingDate, startTime, endTime, roomNumber);

        // CheckRoomAvailability
        if (error == null && !roomIsAvailable(roomNumber, meetingDate, startTime + ":00", endTime + ":00")) {
            error = "Room " + roomNumber + " is already booked during that time slot.";
        }

        if (error != null) {
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", error);
            model.addAttribute("meetingName", meetingName);
            model.addAttribute("meetingDate", meetingDate);
            model.addAttribute("startTime", startTime);
            model.addAttribute("endTime", endTime);
            model.addAttribute("roomNumber", roomNumber);
            addFormOptions(model);
            return "create-meeting";
        }

        Meeting meeting = new Meeting(0, meetingName.trim(),
                java.sql.Date.valueOf(meetingDate), roomNumber);
        meeting.setCreatorID(user.getUserID());
        meeting.setStartTime(Time.valueOf(startTime + ":00"));
        meeting.setEndTime(Time.valueOf(endTime + ":00"));
        meeting.setStatus("Scheduled");

        meetingService.createMeeting(meeting);

        redirectAttributes.addFlashAttribute("successMessage",
                "Meeting \"" + meeting.getMeetingName() + "\" created.");
        return "redirect:/meetings";
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

    private boolean roomIsAvailable(int roomNumber, String date, String startFull, String endFull) {
        Integer conflicts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Meetings WHERE roomNumber = ? AND meetingDate = ? "
                        + "AND startTime < ? AND endTime > ?",
                Integer.class, roomNumber, date, endFull, startFull);
        return conflicts == null || conflicts == 0;
    }
}
