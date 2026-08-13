package com.pennstatesoft.pmss.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserTest {

    /** Minimal concrete subclass that passes arguments straight through to User. */
    private static class TestUser extends User {
        TestUser(int userID, String userEmail, String passwordHash, String firstName,
                 String lastName, String role, String displayName, String birthDate,
                 byte[] profileImage, int failedAttempts, LocalDateTime lastFailed,
                 LocalDateTime lockedTimeTo) {
            super(userID, userEmail, passwordHash, firstName, lastName, role, displayName,
                    birthDate, profileImage, failedAttempts, lastFailed, lockedTimeTo);
        }
    }

    @Test
    void gettersReturnConstructorValues() {
        byte[] img = {1, 2, 3};
        LocalDateTime lastFailed = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime lockedTo = LocalDateTime.of(2026, 1, 1, 10, 15);

        User user = new TestUser(5, "a@pennstatesoft.com", "hash", "Ada", "Lovelace",
                "CLIENT", "Ada L.", "1990-12-10", img, 2, lastFailed, lockedTo);

        assertEquals(5, user.getUserID());
        assertEquals("a@pennstatesoft.com", user.getUserEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals("Ada", user.getFirstName());
        assertEquals("Lovelace", user.getLastName());
        assertEquals("CLIENT", user.getRole());
        assertEquals("Ada L.", user.getDisplayName());
        assertEquals("1990-12-10", user.getBirthDate());
        assertArrayEquals(img, user.getProfileImage());
        assertEquals(2, user.getFailedAttempts());
        assertEquals(lastFailed, user.getLastFailedLogin());
        assertEquals(lockedTo, user.getLockedTimeTo());
    }

    @Test
    void loginTrackingSettersRoundTrip() {
        User user = new TestUser(1, "a@pennstatesoft.com", "hash", "F", "L",
                "CLIENT", "d", "b", null, 0, null, null);

        LocalDateTime lastFailed = LocalDateTime.of(2026, 2, 2, 8, 0);
        LocalDateTime lockedTo = LocalDateTime.of(2026, 2, 2, 8, 15);

        user.setFailedAttempts(3);
        user.setLastFailedLogin(lastFailed);
        user.setLockedTimeTo(lockedTo);

        assertEquals(3, user.getFailedAttempts());
        assertEquals(lastFailed, user.getLastFailedLogin());
        assertEquals(lockedTo, user.getLockedTimeTo());

        user.setLockedTimeTo(null);
        assertNull(user.getLockedTimeTo());
    }

    @Test
    void clientWiresPassThroughFields() {
        // Client parameter order is (userID, email, hash, lastName, firstName, role,
        // birthDate, displayName, ...).
        Client client = new Client(9, "c@pennstatesoft.com", "h",
                "Smith", "John", "CLIENT", "1985-03-04", "John S.",
                null, 1, null, null);

        assertEquals(9, client.getUserID());
        assertEquals("c@pennstatesoft.com", client.getUserEmail());
        assertEquals("John", client.getFirstName());
        assertEquals("Smith", client.getLastName());
        assertEquals("CLIENT", client.getRole());
        assertEquals(1, client.getFailedAttempts());
    }

    @Test
    void administratorWiresPassThroughFields() {
        Administrator admin = new Administrator(3, "admin@pennstatesoft.com", "h",
                "Grace", "Hopper", "ADMINISTRATOR", "1980-01-01", "Grace H.",
                null, 0, null, null);

        assertEquals(3, admin.getUserID());
        assertEquals("admin@pennstatesoft.com", admin.getUserEmail());
        assertEquals("Grace", admin.getFirstName());
        assertEquals("Hopper", admin.getLastName());
        assertEquals("ADMINISTRATOR", admin.getRole());
    }

    @Test
    void administratorConstructorPassesBirthDateAndDisplayNameInSwappedOrder() {
        // Characterization test: Administrator/Client forward birthDate and displayName
        // to the User constructor in the opposite order to the parameters it declares,
        // so the two values end up swapped. Documenting the actual behaviour here.
        Administrator admin = new Administrator(3, "admin@pennstatesoft.com", "h",
                "Grace", "Hopper", "ADMINISTRATOR", "1980-01-01", "Grace H.",
                null, 0, null, null);

        assertEquals("1980-01-01", admin.getDisplayName());
        assertEquals("Grace H.", admin.getBirthDate());
    }
}
