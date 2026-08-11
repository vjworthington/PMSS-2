package com.pennstatesoft.pmss.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogger {
    private static final Logger logger = LoggerFactory.getLogger("PMSS_SECURITY");
    // Logging in/out/denied
    public void loginSuccess(String email) {
        logger.info("LOGIN_SUCCESS user={}", email);
    }
    public void loginFailure(String email) {
        logger.warn("LOGIN_FAILURE user={}", email);
    }
    public void logout(String email) {
        logger.info("LOGOUT user={}", email);
    }

    // PmssAccessDeniedHandler
    public void authorizationFailure(String email, String path) {
        logger.warn("AUTHORIZATION_FAILURE user={} path={}", email, path);
    }

    // Create admin account
    public void adminAccountCreated(String creatorEmail, String newAdminEmail) {
        logger.warn("ADMIN_ACCOUNT_CREATED creator={} newAdmin={}", creatorEmail, newAdminEmail);
    }

    // Profile change
    public void profileChanged(String email) {
        logger.info("PROFILE_CHANGED user={}", email);
    }

}