package com.pennstatesoft.pmss.model;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Meeting
{
    private int meetingID;
    private String meetingName;
    private Date meetingDate;
    private Time startTime;
    private Time endTime;
    private int creatorID;
    private int roomNumber;
    private List<Client> attendees;
    private String status;

    public Meeting(
            int meetingID,
            String meetingName,
            Date meetingDate,
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

    public Date getMeetingDate()
    {
        return meetingDate;
    }

    public void setMeetingDate(Date meetingDate)
    {
        this.meetingDate = meetingDate;
    }

    public Time getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Time startTime)
    {
        this.startTime = startTime;
    }

    public Time getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Time endTime)
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

}