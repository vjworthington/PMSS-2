package com.pennstatesoft.pmss.controller;

import java.time.LocalDate;
/**
 * Controller interface referenced by RegisterDashboard
 * Declares the account-creation prototypes implemented by the concrete
 * RegisterController
 */
public interface RegisterControllerIF {

    boolean registerAccount(String email, String password, LocalDate birthDate);

    boolean checkEmailUnique(String email);

    void createClient();
}
