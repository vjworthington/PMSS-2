package com.pennstatesoft.pmss.model;

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

    public User(int userID, String userEmail, String passwordHash, String firstName, String lastName, String role, String displayName, String birthDate, byte[] profileImage) {

        this.userID = userID;
        this.userEmail = userEmail;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.birthDate = birthDate;
        this.displayName = displayName;
        this.profileImage = profileImage;
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
}