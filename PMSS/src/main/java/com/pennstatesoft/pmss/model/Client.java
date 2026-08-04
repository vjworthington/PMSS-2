package com.pennstatesoft.pmss.model;

public class Client extends User {

    public Client(int userID,
                  String userEmail,
                  String passwordHash,
                  String lastName,
                  String firstName,
                  String role) {

        super(userID, userEmail, passwordHash, firstName, lastName, role);
    }

    /** TODO: Remove upon completion:
     * Administrators can do everything a client do, so all methods
     * should live in the parent User abstract class.
     */
}