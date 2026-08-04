package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Administrator;
import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {

        String role = rs.getString("role");

        if ("CLIENT".equals(role)) {
            return new Client(
                    rs.getInt("userID"),
                    rs.getString("userEmail"),
                    rs.getString("passwordHash"),
                    rs.getString("lastName"),
                    rs.getString("firstName"),
                    role
            );
        }

        else if ("ADMINISTRATOR".equals(role)) {
            return new Administrator(
                    rs.getInt("userID"),
                    rs.getString("userEmail"),
                    rs.getString("passwordHash"),
                    rs.getString("lastName"),
                    rs.getString("firstName"),
                    role
            );
        }

        throw new IllegalArgumentException("Unknown role: " + role);
    }
}