package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Meeting;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;

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

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 5) {
            trimmed = trimmed + ":00";
        }
        return LocalTime.parse(trimmed);
    }
}
