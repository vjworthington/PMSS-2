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

        System.out.println(
                jdbcTemplate.queryForList(
                        "SELECT name FROM sqlite_master WHERE type='table'"
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
}