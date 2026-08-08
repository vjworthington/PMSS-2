package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.User;

import java.util.List;

/**
 * Controller interface referenced by ScheduleDashboard
 * Limits Clients to only the meeting-retrieval functions they are allowed to call.
 */
public interface ScheduleControllerIF {

    List<Meeting> getCreatedMeetings(User user);

    List<Meeting> getParticipatingMeetings(User user);

    List<Meeting> getUsersMeetings(User user);
}
