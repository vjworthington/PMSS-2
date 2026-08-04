package com.pennstatesoft.pmss.controller;

//import Database.DatabaseConnect;
//import Model.Client;

import com.pennstatesoft.pmss.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileController {

    public boolean validateProfile(Client client) {

        return client != null;
    }

    public boolean updateProfile(Client client) {

        return true;
    }

    public Client retrieveProfile(int userID) {

        String sql = """
                SELECT *
                FROM Users
                WHERE userID = ? AND role = 'CLIENT';
                """;

        /**
        try (Connection conn = DatabaseConnect.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userID);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                return new Client(
                        rs.getInt("userID"),
                        rs.getString("userEmail"),
                        rs.getString("passwordHash"),
                        rs.getString("firstName"),
                        rs.getString("lastName")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        **/

        return null;
    }
}