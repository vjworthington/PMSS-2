package com.pennstatesoft.pmss.model;

import java.util.Date;

public class Complaint {
    private int complaint;
    private int meetingID;
    private int clientID;
    private String complaintOption;
    private String summary;
    private String status;
    private String adminResponse;
    private Date dateFiled;

    public Complaint(int clientID,
                     int meetingID,
                     String complaintOption,
                     String summary) {

        this.clientID = clientID;
        this.meetingID = meetingID;
        this.complaintOption = complaintOption;
        this.summary = summary;
    }

    public void updateStatus(String newStatus) {
    }

    public void setAdminResponse(String adminResponse) {
    }

    public int getComplaintID() {
        return complaint;
    }

    public String getStatus() {
        return status;
    }
}
