package com.pennstatesoft.pmss.model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private int roomNumber;
    private boolean isOccupied;
    private List<Meeting> meetings;

    public Room(int roomNumber) {
        this.roomNumber = roomNumber;
        this.isOccupied = false;
        this.meetings = new ArrayList<Meeting>();
    }

    public int getRoomNumber() {
        return roomNumber;
    }
    public void setRoomNumber(int number) { this.roomNumber = number; }
    public boolean isOccupied() {
        return isOccupied;
    }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
    public List<Meeting> getMeetings() {
        return meetings;
    }
    public void addMeeting(Meeting meeting) { meetings.add(meeting); }
}
