package com.pennstatesoft.pmss.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;

public class Meeting
{
    private int meetingID;
    private LocalDate meetingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int creatorID;
    private int roomNumber;
    private List<Client> attendees;
    private String status;
    private int roomId;
    private String meetingName;

    public Meeting(
            int meetingID,
            String meetingName,
            LocalDate meetingDate,
            int roomNumber)
    {
        this.meetingID = meetingID;
        this.meetingName = meetingName;
        this.meetingDate = meetingDate;
        this.roomNumber = roomNumber;

        this.attendees = new ArrayList<>();
    }

    public int getMeetingID()
    {
        return meetingID;
    }

    public String getMeetingName()
    {
        return meetingName;
    }

    public void setMeetingName(String meetingName)
    {
        this.meetingName = meetingName;
    }

    public LocalDate getMeetingDate()
    {
        return meetingDate;
    }

    public void setMeetingDate(LocalDate meetingDate)
    {
        this.meetingDate = meetingDate;
    }

    public LocalTime getStartTime()
    {
        return startTime;
    }

    public void setStartTime(LocalTime startTime)
    {
        this.startTime = startTime;
    }

    public LocalTime getEndTime()
    {
        return endTime;
    }

    public void setEndTime(LocalTime endTime)
    {
        this.endTime = endTime;
    }

    public int getCreatorID()
    {
        return creatorID;
    }

    public void setCreatorID(int creatorID)
    {
        this.creatorID = creatorID;
    }

    public int getRoomNumber()
    {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber)
    {
        this.roomNumber = roomNumber;
    }

    public List<Client> getAttendees()
    {
        return attendees;
    }

    public boolean addAttendee(Client client)
    {
        if(client == null)
        {
            return false;
        }

        if(attendees.contains(client))
        {
            return false;
        }

        attendees.add(client);

        return true;
    }

    public boolean removeAttendee(int userID)
    {
        return attendees.removeIf(
                client -> client.getUserID() == userID
        );
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public int getRoomId() {
        return roomId;
    }

}