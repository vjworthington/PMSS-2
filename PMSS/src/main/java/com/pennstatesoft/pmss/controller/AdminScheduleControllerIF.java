package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.Room;
import com.pennstatesoft.pmss.model.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


/**
 * Controller interface referenced by AdminScheduleDashboard
 * Inherits all Client functions from ScheduleControllerIF and adds the
 * Administrator-only filtering functions.
 */
public interface AdminScheduleControllerIF extends ScheduleControllerIF {

    List<Meeting> getMeetingsByDay(LocalDate date);

    List<Meeting> getMeetingsByWeek(LocalDate weekStart);

    List<Meeting> getMeetingsByRoom(Room room);

    List<Meeting> getMeetingsByPerson(User user);

    List<Meeting> getMeetingsByTimeSlot(LocalTime startTime, LocalTime endTime);
}
