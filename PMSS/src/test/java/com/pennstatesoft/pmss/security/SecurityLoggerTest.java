package com.pennstatesoft.pmss.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * SecurityLogger only emits log lines, so these are smoke tests confirming every
 * logging method is callable without error (including null arguments).
 */
class SecurityLoggerTest {

    private final SecurityLogger logger = new SecurityLogger();

    @Test
    void allLoggingMethodsExecuteWithoutThrowing() {
        assertDoesNotThrow(() -> {
            logger.loginSuccess("a@x.com");
            logger.loginFailure("a@x.com");
            logger.logout("a@x.com");
            logger.authorizationFailure("a@x.com", "/admin");
            logger.adminAccountCreated("admin@x.com", "new@x.com");
            logger.clientAccountCreated("a@x.com");
            logger.profileChanged("a@x.com");
            logger.roomCreated("admin@x.com", 5);
            logger.roomDeleted("admin@x.com", 5);
            logger.meetingCreated("a@x.com", "Sync");
            logger.meetingDeleted("a@x.com", 3);
            logger.attendeeAdded("a@x.com", 3, 9);
            logger.attendeeRemoved("a@x.com", 3, 9);
            logger.billingUpdated("a@x.com", 9);
            logger.complaintFiled("a@x.com", 3);
            logger.complaintResolved("admin@x.com", 3);
            logger.accountLocked("a@x.com");
        });
    }

    @Test
    void toleratesNullArguments() {
        assertDoesNotThrow(() -> {
            logger.loginFailure(null);
            logger.accountLocked(null);
            logger.authorizationFailure(null, null);
        });
    }
}
