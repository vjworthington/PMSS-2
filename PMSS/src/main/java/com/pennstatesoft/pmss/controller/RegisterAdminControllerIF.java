package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Administrator;

/**
 * Controller interface referenced by RegisterAdminDashboard
 * Inherits the Client-facing prototypes from RegisterControllerIF and adds the
 * Administrator factory used when an Administrator registers another admin account.
 */
public interface RegisterAdminControllerIF extends RegisterControllerIF {

    Administrator createAdmin();
}
