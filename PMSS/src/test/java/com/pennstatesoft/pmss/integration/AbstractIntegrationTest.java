package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shared base for integration tests.
 *
 * <p>Boots the whole Spring context (controllers, services, repositories, Spring
 * Security, the JDBC layer) against an isolated in-memory SQLite database
 * configured by {@code application-test.properties}. Because every test class
 * shares this exact configuration, Spring caches and reuses a single context,
 * and the in-memory database lives for the whole test run.
 *
 * <p>{@link #resetDatabase()} runs before each test so a mutation in one test
 * never leaks into another. Seeding goes through the real {@link PasswordEncoder}
 * bean, so seeded passwords authenticate exactly like production accounts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String CLIENT_EMAIL = "client@pennstatesoft.com";
    protected static final String ADMIN_EMAIL = "admin@pennstatesoft.com";
    protected static final String RAW_PASSWORD = "Password1!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected int clientId;
    protected int adminId;
    protected int regularRoom;
    protected int specialRoom;

    @BeforeEach
    void resetDatabase() {
        // Order matters: children before parents to respect foreign keys.
        jdbc.update("DELETE FROM MeetingAttendees");
        jdbc.update("DELETE FROM Complaints");
        jdbc.update("DELETE FROM Meetings");
        jdbc.update("DELETE FROM Billing");
        jdbc.update("DELETE FROM Users");
        jdbc.update("DELETE FROM Rooms");
        // Reset AUTOINCREMENT counters so IDs are predictable across tests.
        jdbc.update("DELETE FROM sqlite_sequence");

        clientId = seedUser(CLIENT_EMAIL, "CLIENT", "Casey", "Client");
        adminId = seedUser(ADMIN_EMAIL, "ADMINISTRATOR", "Adam", "Admin");
        regularRoom = seedRoom("REGULAR", 0.0);
        specialRoom = seedRoom("SPECIAL", 150.0);

        seedExtra();
    }

    /** Hook for subclasses that need additional fixtures. Default: none. */
    protected void seedExtra() {
        // no-op
    }

    protected int seedUser(String email, String role, String firstName, String lastName) {
        jdbc.update(
                "INSERT INTO Users (userEmail, passwordHash, lastName, firstName, role, displayName, birthDate) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                email,
                passwordEncoder.encode(RAW_PASSWORD),
                lastName,
                firstName,
                role,
                firstName + " " + lastName,
                "1990-01-01");
        return jdbc.queryForObject(
                "SELECT userID FROM Users WHERE userEmail = ?", Integer.class, email);
    }

    protected int seedRoom(String roomType, double fee) {
        jdbc.update(
                "INSERT INTO Rooms (isOccupied, fee, roomType) VALUES (0, ?, ?)",
                fee, roomType);
        return jdbc.queryForObject(
                "SELECT MAX(roomNumber) FROM Rooms", Integer.class);
    }

    protected int seedMeeting(String name, String date, String start, String end,
                              int creatorId, int roomNumber) {
        jdbc.update(
                "INSERT INTO Meetings (meetingName, meetingDate, startTime, endTime, userID, roomNumber, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'SCHEDULED')",
                name, date, start, end, creatorId, roomNumber);
        return jdbc.queryForObject(
                "SELECT MAX(meetingID) FROM Meetings", Integer.class);
    }

    protected int countMeetings() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM Meetings", Integer.class);
    }

    protected void seedAttendee(int meetingId, int userId) {
        jdbc.update("INSERT INTO MeetingAttendees (meetingID, userID) VALUES (?, ?)",
                meetingId, userId);
    }

    protected int seedComplaint(int userId, Integer meetingId, String option,
                                String summary, String status) {
        // Store dateFiled the way the app does (SQLite datetime('now'): no millis),
        // so the ComplaintRowMapper is exercised against a realistic value.
        jdbc.update(
                "INSERT INTO Complaints (meetingID, userID, complaintOption, summary, status, dateFiled) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                meetingId, userId, option, summary, status, "2026-08-01 09:00:00");
        return jdbc.queryForObject("SELECT MAX(complaintID) FROM Complaints", Integer.class);
    }
}
