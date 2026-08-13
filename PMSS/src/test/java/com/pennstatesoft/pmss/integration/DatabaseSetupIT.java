package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test: proves the isolated in-memory schema is created and seeded, and
 * that seeded credentials are BCrypt-hashed by the real encoder.
 */
class DatabaseSetupIT extends AbstractIntegrationTest {

    @Test
    void seedsUsersAndRooms() {
        assertEquals(2, (int) jdbc.queryForObject("SELECT COUNT(*) FROM Users", Integer.class));
        assertEquals(2, (int) jdbc.queryForObject("SELECT COUNT(*) FROM Rooms", Integer.class));
        assertEquals(0, countMeetings());
    }

    @Test
    void seededPasswordsAreBcryptHashed() {
        String hash = jdbc.queryForObject(
                "SELECT passwordHash FROM Users WHERE userEmail = ?", String.class, CLIENT_EMAIL);
        assertTrue(hash.startsWith("$2"), "password should be BCrypt hashed");
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, hash));
    }
}
