package com.pennstatesoft.pmss.model;

import java.time.LocalDate;
import java.time.ZoneId;

public class Complaint {
    private int complaintID;
    private int meetingID;
    private int userID;
    private String complaintOption;
    private String summary;
    private String status;
    private String adminResponse;
    private LocalDate dateFiled;

    public Complaint(int userID,
                     int meetingID,
                     String complaintOption,
                     String summary) {

        this.userID = userID;
        this.meetingID = meetingID;
        this.complaintOption = complaintOption;
        this.summary = summary;
        this.status = "PENDING";
        this.dateFiled = LocalDate.now(ZoneId.systemDefault());
    }

    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
        this.status = "RESOLVED";
    }

    public void restoreAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }

    public int getComplaintID() {
        return complaintID;
    }

    public void setComplaintID(int complaintID){
        this.complaintID = complaintID;
    }

    public int getMeetingID() {
        return meetingID;
    }

    public void setMeetingID(int meetingID){
        this.meetingID = meetingID;
    }

    public int getUserID() {
        return userID;
    }

    public String getComplaintOption() {
        return complaintOption;
    }

    public void setComplaintOption(String complaintOption){
        this.complaintOption = complaintOption;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary){
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public LocalDate getDateFiled() {
        return dateFiled;
    }

    public void setDateFiled(LocalDate dateFiled){
        this.dateFiled = dateFiled;
    }
}
