package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Administrator;
import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Component
public class UserRowMapper implements RowMapper<User> {
    private static final String FAILED_ATTEMPTS = "failedAttempts";

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {

        String role = rs.getString("role");
        int failedAttempts = rs.getInt(FAILED_ATTEMPTS);
        String lastFailedString = rs.getString("lastFailedLogin");
        LocalDateTime lastFailed = lastFailedString == null
                ? null
                : LocalDateTime.parse(lastFailedString);
        String lockedTimeToString = rs.getString("lockedTimeTo");
        LocalDateTime lockedTimeTo = lockedTimeToString == null
                ? null
                : LocalDateTime.parse(lockedTimeToString);
        User user;

        if ("CLIENT".equals(role)) {
            user = new Client(
                    rs.getInt("userID"),
                    rs.getString("userEmail"),
                    rs.getString("passwordHash"),
                    rs.getString("lastName"),
                    rs.getString("firstName"),
                    role,
                    rs.getString("displayName"),
                    rs.getString("birthDate"),
                    rs.getBytes("profileImage"),
                    rs.getInt(FAILED_ATTEMPTS),
                    lastFailed,
                    lockedTimeTo
            );
        }

        else if ("ADMINISTRATOR".equals(role)) {
            user = new Administrator(
                    rs.getInt("userID"),
                    rs.getString("userEmail"),
                    rs.getString("passwordHash"),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    role,
                    rs.getString("displayName"),
                    rs.getString("birthDate"),
                    rs.getBytes("profileImage"),
                    rs.getInt(FAILED_ATTEMPTS),
                    lastFailed,
                    lockedTimeTo
            );
        } else {
            throw new IllegalArgumentException("Unknown role: " + role);
        }

        user.setFailedAttempts(failedAttempts);
        user.setLastFailedLogin(lastFailed);
        user.setLockedTimeTo(lockedTimeTo);

        return user;
    }
}