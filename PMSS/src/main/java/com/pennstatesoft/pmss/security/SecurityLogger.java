package com.pennstatesoft.pmss.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogger {
    private static final Logger logger = LoggerFactory.getLogger("PMSS_SECURITY");

    // Logging in/out/denied
    public void loginSuccess(String email) {
        logger.info("LOGIN_SUCCESS user={}", email);
    }
    public void loginFailure(String email) {
        logger.warn("LOGIN_FAILURE user={}", email);
    }
    public void logout(String email) {
        logger.info("LOGOUT user={}", email);
    }

    // PmssAccessDeniedHandler
    public void authorizationFailure(String email, String path) {
        logger.warn("AUTHORIZATION_FAILURE user={} path={}", email, path);
    }

    // Create admin account
    public void adminAccountCreated(String creatorEmail, String newAdminEmail) {
        logger.warn("ADMIN_ACCOUNT_CREATED creator={} newAdmin={}", creatorEmail, newAdminEmail);
    }

    // Create client account
    public void clientAccountCreated(String email) {
        logger.info("CLIENT_ACCOUNT_CREATED user={}", email);
    }

    // Profile change
    public void profileChanged(String email) {
        logger.info("PROFILE_CHANGED user={}", email);
    }

    // Create New Room
    public void roomCreated(String adminEmail, int roomNumber) {
        logger.info("ROOM_CREATED admin={} room={}", adminEmail, roomNumber);
    }

    // Delete Room
    public void roomDeleted(String adminEmail, int roomNumber) {
        logger.warn("ROOM_DELETED admin={} room={}", adminEmail, roomNumber);
    }

    // Create Meeting
    public void meetingCreated(String email, String meetingName) {
        logger.info("MEETING_CREATED user={} meeting=\"{}\"", email, meetingName);
    }

    // Delete Meeting
    public void meetingDeleted(String email, int meetingID) {
        logger.warn("MEETING_DELETED user={} meetingID={}", email, meetingID);
    }

    // Add Attendee to meeting
    public void attendeeAdded(String email, int meetingID, int participantID) {
        logger.info("ATTENDEE_ADDED user={} meetingID={} participant={}", email, meetingID, participantID);
    }

    // Remove Attendee from meeting
    public void attendeeRemoved(String email, int meetingID, int participantID) {
        logger.info("ATTENDEE_REMOVED user={} meetingID={} participant={}", email, meetingID, participantID);
    }

    // Update billing information
    public void billingUpdated(String actorEmail, int clientID) {
        logger.info("BILLING_UPDATED by={} client={}", actorEmail, clientID);
    }

    // Filed Complaint
    public void complaintFiled(String email, int meetingID) {
        logger.info("COMPLAINT_FILED user={} meetingID={}", email, meetingID);
    }

    // Resolved Complaint
    public void complaintResolved(String adminEmail, int complaintID) {
        logger.info("COMPLAINT_RESPONDED admin={} complaintID={}", adminEmail, complaintID);
    }

    // Locked Account
    public void accountLocked(String email) {
        logger.warn("ACCOUNT_LOCKED user={} duration=15_minutes",email);
    }
}
