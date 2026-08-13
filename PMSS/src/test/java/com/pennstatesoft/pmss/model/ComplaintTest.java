package com.pennstatesoft.pmss.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComplaintTest {

    @Test
    void constructorSetsFieldsAndDefaults() {
        Complaint complaint = new Complaint(7, 42, "Billing", "Charged twice");

        assertEquals(7, complaint.getUserID());
        assertEquals(42, complaint.getMeetingID());
        assertEquals("Billing", complaint.getComplaintOption());
        assertEquals("Charged twice", complaint.getSummary());
        assertEquals("PENDING", complaint.getStatus(), "new complaints default to PENDING");
        assertNull(complaint.getAdminResponse());
        assertNotNull(complaint.getDateFiled(), "dateFiled is stamped in the constructor");
    }

    @Test
    void updateStatusChangesStatusOnly() {
        Complaint complaint = new Complaint(1, 0, "Other", "text");

        complaint.updateStatus("IN_PROGRESS");

        assertEquals("IN_PROGRESS", complaint.getStatus());
        assertNull(complaint.getAdminResponse());
    }

    @Test
    void setAdminResponseAlsoMarksResolved() {
        Complaint complaint = new Complaint(1, 0, "Other", "text");

        complaint.setAdminResponse("We refunded you.");

        assertEquals("We refunded you.", complaint.getAdminResponse());
        assertEquals("RESOLVED", complaint.getStatus(),
                "setting a response transitions the complaint to RESOLVED");
    }

    @Test
    void restoreAdminResponseDoesNotChangeStatus() {
        Complaint complaint = new Complaint(1, 0, "Other", "text");

        complaint.restoreAdminResponse("previously stored response");

        assertEquals("previously stored response", complaint.getAdminResponse());
        assertEquals("PENDING", complaint.getStatus(),
                "restoring a response (used when loading from DB) must not resolve it");
    }

    @Test
    void settersAndGettersRoundTrip() {
        Complaint complaint = new Complaint(1, 0, "Other", "text");
        LocalDate filed = LocalDate.of(1970, 1, 1);

        complaint.setComplaintID(99);
        complaint.setMeetingID(5);
        complaint.setComplaintOption("Room Condition");
        complaint.setSummary("Too cold");
        complaint.setDateFiled(filed);

        assertEquals(99, complaint.getComplaintID());
        assertEquals(5, complaint.getMeetingID());
        assertEquals("Room Condition", complaint.getComplaintOption());
        assertEquals("Too cold", complaint.getSummary());
        assertEquals(filed, complaint.getDateFiled());
    }
}
