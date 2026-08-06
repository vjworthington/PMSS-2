package com.pennstatesoft.pmss.model;

public class Client extends User {

    public Client(int userID,
                  String userEmail,
                  String passwordHash,
                  String lastName,
                  String firstName,
                  String role,
                  String birthDate,
                  String displayName,
                  byte[] profileImage) {

        super(userID, userEmail, passwordHash, lastName, firstName, role, birthDate, displayName, profileImage);
    }

    /** TODO: Remove upon completion:
     * Administrators can do everything a client do, so all methods
     * should live in the parent User abstract class.
     */
}