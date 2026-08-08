package com.pennstatesoft.pmss.controller;

/**
 * Controller interface referenced by RegisterAdminDashboard
 * Inherits the Client-facing prototypes from RegisterControllerIF and adds the
 * Administrator factory used when an Administrator registers another admin account.
 */
public interface RegisterAdminControllerIF extends RegisterControllerIF {

    void createAdmin();
}
