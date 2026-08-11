package com.pennstatesoft.pmss.repository;

import com.pennstatesoft.pmss.model.Meeting;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MeetingRepository
{
    private final JdbcTemplate jdbcTemplate;

    public MeetingRepository(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Create a new meeting
    public int createMeeting(Meeting meeting)
    {
        String sql = """
                INSERT INTO Meetings
                (
                    meetingName,
                    meetingDate,
                    startTime,
                    endTime,
                    userID,
                    roomNumber,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        return jdbcTemplate.update(
                sql,
                meeting.getMeetingName(),
                meeting.getMeetingDate(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                meeting.getCreatorID(),
                meeting.getRoomNumber(),
                meeting.getStatus()
        );

    }

    public Meeting findById(int meetingID)
    {
        String sql = """
                SELECT *
                FROM Meetings
                WHERE meetingID = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                new MeetingRowMapper(),
                meetingID
        );
    }

    public List<Meeting> findByCreator(int userID)
    {
        String sql = """
                SELECT *
                FROM Meetings
                WHERE userID = ?
                """;

        return jdbcTemplate.query(
                sql,
                new MeetingRowMapper(),
                userID
        );
    }

    public List<Meeting> findAll()
    {
        String sql = """
                SELECT *
                FROM Meetings
                """;

        return jdbcTemplate.query(
                sql,
                new MeetingRowMapper()
        );
    }

    public int updateMeeting(Meeting meeting)
    {
        String sql = """
                UPDATE Meetings
                SET
                    meetingName=?,
                    meetingDate=?,
                    startTime=?,
                    endTime=?,
                    roomNumber=?,
                    status=?
                WHERE meetingID=?
                """;

        return jdbcTemplate.update(
                sql,
                meeting.getMeetingName(),
                meeting.getMeetingDate(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                meeting.getRoomNumber(),
                meeting.getStatus(),
                meeting.getMeetingID()
        );
    }

    public int deleteMeeting(int meetingID)
    {
        String sql = """
                DELETE FROM Meetings
                WHERE meetingID=?
                """;

        return jdbcTemplate.update(
                sql,
                meetingID
        );
    }
}