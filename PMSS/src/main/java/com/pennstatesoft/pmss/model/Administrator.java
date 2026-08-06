package com.pennstatesoft.pmss.model;

public class Administrator extends User {

    public Administrator(int userID,
                         String userEmail,
                         String passwordHash,
                         String firstName,
                         String lastName,
                         String role,
                         String birthDate,
                         String displayName,
                         byte[] profileImage) {

        super(userID, userEmail, passwordHash, firstName, lastName, role, birthDate, displayName, profileImage);
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