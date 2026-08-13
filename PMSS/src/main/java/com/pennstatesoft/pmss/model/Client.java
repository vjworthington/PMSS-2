package com.pennstatesoft.pmss.model;

import java.time.LocalDateTime;

public class Client extends User {

    public Client(int userID,
                  String userEmail,
                  String passwordHash,
                  String lastName,
                  String firstName,
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

}