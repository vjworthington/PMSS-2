package com.pennstatesoft.pmss.service;

import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.repository.MeetingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeetingService
{
    private final MeetingRepository meetingRepository;

    public MeetingService(MeetingRepository meetingRepository)
    {
        this.meetingRepository = meetingRepository;
    }

    public boolean createMeeting(Meeting meeting)
    {
        if(meeting == null)
        {
            return false;
        }

        return meetingRepository.createMeeting(meeting) > 0;
    }

    public Meeting getMeeting(int meetingID)
    {
        return meetingRepository.findById(meetingID);
    }

    public List<Meeting> getAllMeetings()
    {
        return meetingRepository.findAll();
    }

    public List<Meeting> getUserMeetings(int userID)
    {
        return meetingRepository.findByCreator(userID);
    }

    public boolean updateMeeting(Meeting meeting)
    {
        if(meeting == null)
        {
            return false;
        }

        return meetingRepository.updateMeeting(meeting) > 0;
    }

    public boolean deleteMeeting(int meetingID)
    {
        return meetingRepository.deleteMeeting(meetingID) > 0;
    }

    @Transactional
    public void scheduleMeeting(Meeting meeting) {

        if (!meetingRepository.isRoomAvailable(
                meeting.getRoomId(),
                meeting.getStartTime(),
                meeting.getEndTime())) {

            throw new IllegalStateException("Room is already booked.");
        }

        meetingRepository.createMeeting(meeting);    }
}