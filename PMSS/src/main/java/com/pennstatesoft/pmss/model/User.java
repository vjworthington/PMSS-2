package com.pennstatesoft.pmss.model;

import java.time.LocalDateTime;

public abstract class User {

    protected int userID;
    protected String userEmail;
    protected String passwordHash;
    protected String lastName;
    protected String firstName;
    protected String role;
    protected String displayName;
    protected String birthDate;
    protected byte[] profileImage;
    protected int failedAttempts;
    protected LocalDateTime lastFailed;
    protected LocalDateTime lockedTimeTo;

    public User(int userID, String userEmail, String passwordHash, String firstName, String lastName, String role, String displayName, String birthDate, byte[] profileImage, int failedAttempts, LocalDateTime lastFailed, LocalDateTime lockedTimeTo) {

        this.userID = userID;
        this.userEmail = userEmail;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.birthDate = birthDate;
        this.displayName = displayName;
        this.profileImage = profileImage;
        this.failedAttempts = failedAttempts;
        this.lastFailed = lastFailed;
        this.lockedTimeTo = lockedTimeTo;
    }

    public int getUserID() {
        return userID;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void payment() {
        // Processing payment section
    }

    public void manageMeeting() {
        // Managing meeting section
    }

    public void fileComplaint() {
        // Filing complaint section
    }

    public String getRole() {
        return role;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public LocalDateTime getLastFailedLogin() {
        return lastFailed;
    }

    public LocalDateTime getLockedTimeTo() {
        return lockedTimeTo;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public void setLastFailedLogin(LocalDateTime lastFailed) {
        this.lastFailed = lastFailed;
    }

    public void setLockedTimeTo(LocalDateTime lockedTimeTo) {
        this.lockedTimeTo = lockedTimeTo;
    }
}