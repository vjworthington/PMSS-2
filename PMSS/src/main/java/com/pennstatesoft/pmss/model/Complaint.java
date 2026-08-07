package com.pennstatesoft.pmss.model;

import java.util.Date;

public class Complaint {
    private int complaintID;
    private int meetingID;
    private int userID;
    private String complaintOption;
    private String summary;
    private String status;
    private String adminResponse;
    private Date dateFiled;

    public Complaint(int userID,
                     int meetingID,
                     String complaintOption,
                     String summary) {

        this.userID = userID;
        this.meetingID = meetingID;
        this.complaintOption = complaintOption;
        this.summary = summary;
        this.status = "PENDING";
        this.dateFiled = new Date();
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

    public Date getDateFiled() {
        return dateFiled;
    }

    public void setDateFiled(Date dateFiled){
        this.dateFiled = dateFiled;
    }
}
