package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public User findByEmail(String email) {

        System.out.println(
                "USERS IN DATABASE: " +
                jdbcTemplate.queryForList(
                        "SELECT userID, userEmail, role FROM Users"
                )
        );

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
                image = ?
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