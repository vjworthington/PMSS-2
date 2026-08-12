package com.pennstatesoft.pmss.model;

import java.time.LocalDateTime;

public class Administrator extends User {

    public Administrator(int userID,
                         String userEmail,
                         String passwordHash,
                         String firstName,
                         String lastName,
                         String role,
                         String birthDate,
                         String displayName,
                         byte[] profileImage,
                         int failedAttempts,
                         LocalDateTime lastFailed,
                         LocalDateTime lockedTimeTo) {

            super(userID, userEmail, passwordHash, firstName, lastName, role, birthDate, displayName, profileImage, failedAttempts, lastFailed, lockedTimeTo);
        this.failedAttempts = failedAttempts;
        this.lastFailed = lastFailed;
        this.lockedTimeTo = lockedTimeTo;
    }

    public void manageComplaint() {
        // Managing complaints section
    }

    public void manageBilling() {
        // Managing billing section
    }

    public void manageRoom() {

        // Managing rooms section
    }

    public void addAdmin() {

        // Adding administrator section
    }

}