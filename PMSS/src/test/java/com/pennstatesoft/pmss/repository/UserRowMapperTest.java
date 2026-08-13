package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Administrator;
import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.User;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRowMapperTest {

    private final UserRowMapper mapper = new UserRowMapper();

    private ResultSet baseRow(String role) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("role")).thenReturn(role);
        when(rs.getInt("userID")).thenReturn(1);
        when(rs.getString("userEmail")).thenReturn("u@pennstatesoft.com");
        when(rs.getString("passwordHash")).thenReturn("hash");
        when(rs.getString("firstName")).thenReturn("First");
        when(rs.getString("lastName")).thenReturn("Last");
        when(rs.getString("displayName")).thenReturn("Display");
        when(rs.getString("birthDate")).thenReturn("2000-01-01");
        when(rs.getBytes("profileImage")).thenReturn(null);
        when(rs.getInt("failedAttempts")).thenReturn(0);
        when(rs.getString("lastFailedLogin")).thenReturn(null);
        when(rs.getString("lockedTimeTo")).thenReturn(null);
        return rs;
    }

    @Test
    void mapsClientRole() throws SQLException {
        User user = mapper.mapRow(baseRow("CLIENT"), 0);

        assertInstanceOf(Client.class, user);
        assertEquals(1, user.getUserID());
        assertEquals("u@pennstatesoft.com", user.getUserEmail());
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
        assertEquals("CLIENT", user.getRole());
    }

    @Test
    void mapsAdministratorRole() throws SQLException {
        User user = mapper.mapRow(baseRow("ADMINISTRATOR"), 0);

        assertInstanceOf(Administrator.class, user);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
        assertEquals("ADMINISTRATOR", user.getRole());
    }

    @Test
    void parsesLockTimestampsWhenPresent() throws SQLException {
        ResultSet rs = baseRow("CLIENT");
        when(rs.getInt("failedAttempts")).thenReturn(2);
        when(rs.getString("lastFailedLogin")).thenReturn("2026-01-01T10:00:00");
        when(rs.getString("lockedTimeTo")).thenReturn("2026-01-01T10:15:00");

        User user = mapper.mapRow(rs, 0);

        assertEquals(2, user.getFailedAttempts());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), user.getLastFailedLogin());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 15), user.getLockedTimeTo());
    }

    @Test
    void leavesLockTimestampsNullWhenAbsent() throws SQLException {
        User user = mapper.mapRow(baseRow("CLIENT"), 0);

        assertNull(user.getLastFailedLogin());
        assertNull(user.getLockedTimeTo());
    }

    @Test
    void unknownRoleThrows() throws SQLException {
        ResultSet rs = baseRow("SUPERUSER");

        assertThrows(IllegalArgumentException.class, () -> mapper.mapRow(rs, 0));
    }
}
