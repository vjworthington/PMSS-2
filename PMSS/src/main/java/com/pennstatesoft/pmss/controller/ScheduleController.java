package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.Room;
import com.pennstatesoft.pmss.model.User;

import java.sql.Time;
import java.util.Date;
import java.util.List;

public class ScheduleController {
    private List<Meeting> meetings;

    public List<Meeting> getCreatedMeetings(User user) {
        return null;
    }

    public List<Meeting> getParticipatingMeetings(User user) {
        return null;
    }

    public List<Meeting> getUsersMeetings(User user) {
        return null;
    }

    public List<Meeting> getMeetingsByDay(Date date) {
        return null;
    }

    public List<Meeting> getMeetingsByWeek(Date weekStart) {
        return null;
    }

    public List<Meeting> getMeetingsByRoom(Room room) {
        return null;
    }

    public List<Meeting> getMeetingsByPerson(User user) {
        return null;
    }

    public List<Meeting> getMeetingsByTimeSlot(Time startTime, Time endTime) {
        return null;
    }
}
