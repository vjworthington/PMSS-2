package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Meeting;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

@Component
public class MeetingRowMapper implements RowMapper<Meeting> {

    @Override
    public Meeting mapRow(ResultSet rs, int rowNum) throws SQLException {

        Meeting meeting = new Meeting(
                rs.getInt("meetingID"),
                rs.getString("meetingName"),
                parseDate(rs.getString("meetingDate")),
                rs.getInt("roomNumber"));

        meeting.setMeetingName(rs.getString("meetingName"));
        meeting.setStartTime(parseTime(rs.getString("startTime")));
        meeting.setEndTime(parseTime(rs.getString("endTime")));
        meeting.setCreatorID(rs.getInt("userID"));
        meeting.setStatus(rs.getString("status"));

        return meeting;
    }

    private Date parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.valueOf(value.trim());
    }

    private Time parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 5) {
            trimmed = trimmed + ":00";
        }
        return Time.valueOf(trimmed);
    }
}
