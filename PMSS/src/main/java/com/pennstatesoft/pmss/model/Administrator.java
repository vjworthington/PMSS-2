package com.pennstatesoft.pmss.model;

public class Administrator extends User {

    public Administrator(int userID,
                         String userEmail,
                         String passwordHash,
                         String firstName,
                         String lastName,
                         String role
    ) {

        super(userID, userEmail, passwordHash, firstName, lastName, role);
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