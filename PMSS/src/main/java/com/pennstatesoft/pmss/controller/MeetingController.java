package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.service.MeetingService;
import com.pennstatesoft.pmss.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
@RequestMapping("/meetings")
public class MeetingController
{
    private final MeetingService meetingService;
    private final UserService userService;

    public MeetingController(
            MeetingService meetingService,
            UserService userService){

        this.meetingService = meetingService;
        this.userService = userService;
    }

    @GetMapping
    public String displayMeetings(
            Model model,
            Authentication authentication)
    {
        User user = userService.findByEmail(authentication.getName());

        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "meetings",
                meetingService.getAllMeetings()
        );

        return "meetings";
    }

    @GetMapping("/create")
    public String showCreateMeetingPage(
            Model model)
    {
        model.addAttribute(
                "meeting",
                new Meeting(
                        0,
                        "",
                        null,
                        0
                )
        );

        return "create-meeting";
    }

    @PostMapping("/create")
    public String createMeeting(
            @RequestParam String meetingName,
            @RequestParam String meetingDate,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam int roomNumber,
            Authentication authentication)
    {
        User user =
                userService.findByEmail(
                        authentication.getName()
                );

        Meeting meeting =
                new Meeting(
                        0,
                        meetingName,
                        java.sql.Date.valueOf(meetingDate),
                        roomNumber
                );

        meeting.setCreatorID(
                user.getUserID()
        );

        meeting.setStartTime(
                Time.valueOf(startTime + ":00")
        );

        meeting.setEndTime(
                Time.valueOf(endTime + ":00")
        );

        meeting.setStatus(
                "Scheduled"
        );

        meetingService.createMeeting(meeting);

        return "redirect:/meetings";

    }
}