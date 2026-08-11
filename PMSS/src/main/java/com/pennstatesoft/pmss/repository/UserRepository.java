package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User findByEmail(String email) {

        String sql = """
        SELECT *
        FROM Users
        WHERE userEmail = ?
        """;

        return jdbcTemplate.queryForObject(
           sql,
           new UserRowMapper(),
           email
            );
    }

    public void updateProfile(
            String email,
            String displayName,
            String birthDate,
            byte[] image) {

        String sql = """
            UPDATE Users
            SET displayName = ?,
                birthDate = ?,
                profileImage = ?
            WHERE userEmail = ?
            """;

        jdbcTemplate.update(
                sql,
                displayName,
                birthDate,
                image,
                email
        );
    }

    public void updateProfileWithoutImage(
            String email,
            String displayName,
            String birthDate) {

        String sql = """
            UPDATE Users
            SET displayName = ?,
                birthDate = ?
            WHERE userEmail = ?
            """;

        jdbcTemplate.update(
                sql,
                displayName,
                birthDate,
                email
        );
    }
}