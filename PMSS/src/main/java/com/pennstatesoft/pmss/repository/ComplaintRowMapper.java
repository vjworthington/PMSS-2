package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Complaint;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

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

        java.sql.Date dateFiled = rs.getDate("dateFiled");
        if (dateFiled != null) {
            complaint.setDateFiled(dateFiled.toLocalDate());
        }

        String adminResponse = rs.getString("adminResponse");
        if (adminResponse != null) {
            complaint.restoreAdminResponse(adminResponse);
        }

        return complaint;
    }
}
