package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Complaint;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

@Component
public class ComplaintRowMapper implements RowMapper<Complaint> {

    @Override
    public Complaint mapRow(ResultSet rs, int rowNum) throws SQLException {

        Complaint complaint = new Complaint(
                rs.getInt("userID"),
                rs.getInt("meetingID"),
                rs.getString("complaintOption"),
                rs.getString("summary")
        );

        complaint.setComplaintID(rs.getInt("complaintID"));
        complaint.updateStatus(rs.getString("status"));

        // Read dateFiled as a string and keep the date portion. This tolerates both
        // "yyyy-MM-dd" and SQLite's datetime('now') output "yyyy-MM-dd HH:mm:ss",
        // whereas rs.getDate(...) forces sqlite-jdbc's strict timestamp format and
        // throws "Error parsing time stamp" on the values the app actually stores.
        String dateFiled = rs.getString("dateFiled");
        if (dateFiled != null && !dateFiled.isBlank()) {
            complaint.setDateFiled(LocalDate.parse(dateFiled.trim().substring(0, 10)));
        }

        String adminResponse = rs.getString("adminResponse");
        if (adminResponse != null) {
            complaint.restoreAdminResponse(adminResponse);
        }

        return complaint;
    }
}
