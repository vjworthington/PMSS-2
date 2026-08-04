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
    public void setRoomNumber(int number) { roomNumber = roomNumber; }
    public boolean isOccupied() {
        return true;
    }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
    public List<Meeting> getMeeting() {
        return meetings;
    }
    public void addMeeting(Meeting meeting) { meetings.add(meeting); }
}
