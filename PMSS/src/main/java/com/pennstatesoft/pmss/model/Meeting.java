package com.pennstatesoft.pmss.model;

import java.sql.Time;
import java.util.Date;
import java.util.List;

public class Meeting {
    private int meetingID;
    private String meetingName;
    private Date meetingDate;
    private Time startTime;
    private Time endTime;
    private int creatorID;
    private int roomNumber;
    private List<Client> attendees;
    private String status;

    public Meeting (int meetingID,
                    String meetingName,
                    Date meetingDate,
                    int roomNumber) {

        this.meetingID = meetingID;
        this.meetingDate = meetingDate;
        this.roomNumber = roomNumber;
    }

    public int getMeetingID() {
        return meetingID;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public void setMeetingName(String meetingName){
        this.meetingName = meetingName;
    }

    public Date getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(Date meetingDate){
        this.meetingDate = meetingDate;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime){
        this.startTime = startTime;
    }
    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime){
        this.endTime = endTime;
    }

    public int getCreatorID() {
        return creatorID;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber){
        this.roomNumber = roomNumber;
    }

    public List<Client> getAttendees(){
        return null;
    }

    public boolean addAttendee(Client client){
        return true;
    }

    public boolean removeAttendee(int clientID){
        return true;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }
}
