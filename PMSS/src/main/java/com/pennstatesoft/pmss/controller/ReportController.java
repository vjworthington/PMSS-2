package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.service.UserService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * Administrator reporting
 */
@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    public ReportController(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    @GetMapping
    public String reports(Model model, Authentication authentication) {
        model.addAttribute("user", userService.findByEmail(authentication.getName()));

        model.addAttribute("totalMeetings", count("SELECT COUNT(*) FROM Meetings"));
        model.addAttribute("totalRooms", count("SELECT COUNT(*) FROM Rooms"));
        model.addAttribute("specialRooms", count("SELECT COUNT(*) FROM Rooms WHERE roomType = 'SPECIAL'"));
        model.addAttribute("totalClients", count("SELECT COUNT(*) FROM Users WHERE role = 'CLIENT'"));
        model.addAttribute("totalAdmins", count("SELECT COUNT(*) FROM Users WHERE role = 'ADMINISTRATOR'"));
        model.addAttribute("totalComplaints", count("SELECT COUNT(*) FROM Complaints"));

        model.addAttribute("meetingsByRoom", jdbcTemplate.queryForList(
                "SELECT roomNumber, COUNT(*) AS count FROM Meetings GROUP BY roomNumber ORDER BY roomNumber"));
        model.addAttribute("meetingsByCreator", jdbcTemplate.queryForList("""
                SELECT (u.firstName || ' ' || u.lastName) AS creator, COUNT(*) AS count
                FROM Meetings m JOIN Users u ON m.userID = u.userID
                GROUP BY m.userID
                ORDER BY count DESC
                """));
        model.addAttribute("complaintsByStatus", jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS count FROM Complaints GROUP BY status ORDER BY status"));

        return "admin/reports";
    }

    @GetMapping("/meetings.csv")
    public ResponseEntity<String> meetingsCsv() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.meetingID, m.meetingName, m.meetingDate, m.startTime, m.endTime,
                       m.roomNumber, (u.firstName || ' ' || u.lastName) AS creator, m.status
                FROM Meetings m
                LEFT JOIN Users u ON m.userID = u.userID
                ORDER BY m.meetingDate, m.startTime
                """);
        return csv("meetings.csv",
                new String[]{"meetingID", "meetingName", "meetingDate", "startTime", "endTime",
                        "roomNumber", "creator", "status"},
                rows);
    }

    @GetMapping("/complaints.csv")
    public ResponseEntity<String> complaintsCsv() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT c.complaintID, (u.firstName || ' ' || u.lastName) AS client,
                       c.meetingID, c.complaintOption, c.summary, c.status, c.adminResponse
                FROM Complaints c
                LEFT JOIN Users u ON c.userID = u.userID
                ORDER BY c.complaintID
                """);
        return csv("complaints.csv",
                new String[]{"complaintID", "client", "meetingID", "complaintOption",
                        "summary", "status", "adminResponse"},
                rows);
    }

    @GetMapping("/rooms.csv")
    public ResponseEntity<String> roomsCsv() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT r.roomNumber, r.roomType, r.fee,
                       (SELECT COUNT(*) FROM Meetings m WHERE m.roomNumber = r.roomNumber) AS meetings
                FROM Rooms r
                ORDER BY r.roomNumber
                """);
        return csv("rooms.csv",
                new String[]{"roomNumber", "roomType", "fee", "meetings"},
                rows);
    }

    @GetMapping("/users.csv")
    public ResponseEntity<String> usersCsv() {
        // Excludes sensitive fields (passwordHash, profileImage).
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT userID, firstName, lastName, userEmail, role, displayName, birthDate
                FROM Users
                ORDER BY role, lastName, firstName
                """);
        return csv("users.csv",
                new String[]{"userID", "firstName", "lastName", "userEmail", "role", "displayName", "birthDate"},
                rows);
    }

    // helpers

    private int count(String sql) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    private ResponseEntity<String> csv(String filename, String[] columns, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", columns)).append("\n");
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(escape(row.get(columns[i])));
            }
            sb.append("\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString());
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
