package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserService userService = mock(UserService.class);
    private final ReportController controller = new ReportController(jdbc, userService);

    private Authentication auth() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("admin@pennstatesoft.com");
        when(userService.findByEmail("admin@pennstatesoft.com")).thenReturn(
                new Client(1, "admin@pennstatesoft.com", "h", "L", "F", "ADMINISTRATOR",
                        "2000-01-01", "F L", null, 0, null, null));
        return a;
    }

    @Test
    void reportsAggregatesCountsAndReturnsView() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(5);
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.reports(model, auth());

        assertEquals("admin/reports", view);
        assertEquals(5, model.getAttribute("totalMeetings"));
        assertEquals(5, model.getAttribute("totalClients"));
    }

    @Test
    void meetingsCsvHasHeaderAndEscapesCommas() {
        Map<String, Object> row = new HashMap<>();
        row.put("meetingID", 1);
        row.put("meetingName", "A, B");   // comma requires quoting
        row.put("meetingDate", "2026-01-01");
        row.put("startTime", "09:00");
        row.put("endTime", "10:00");
        row.put("roomNumber", 3);
        row.put("creator", "Jane Doe");
        row.put("status", "SCHEDULED");
        when(jdbc.queryForList(anyString())).thenReturn(List.of(row));

        ResponseEntity<String> response = controller.meetingsCsv();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("text/csv"), response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("meetings.csv"));
        String body = response.getBody();
        assertTrue(body.startsWith("meetingID,meetingName,meetingDate,startTime,endTime,roomNumber,creator,status\n"));
        assertTrue(body.contains("\"A, B\""), "values containing commas must be quoted");
    }

    @Test
    void complaintsCsvRendersNullsAsEmpty() {
        Map<String, Object> row = new HashMap<>();
        row.put("complaintID", 1);
        row.put("client", "Jane Doe");
        row.put("meetingID", null);
        row.put("complaintOption", "Billing");
        row.put("summary", "text");
        row.put("status", "PENDING");
        row.put("adminResponse", null);
        when(jdbc.queryForList(anyString())).thenReturn(List.of(row));

        ResponseEntity<String> response = controller.complaintsCsv();

        String body = response.getBody();
        assertTrue(body.startsWith("complaintID,client,meetingID,complaintOption,summary,status,adminResponse\n"));
        // trailing null adminResponse becomes empty string, so the line ends with a comma
        assertTrue(body.contains("1,Jane Doe,,Billing,text,PENDING,\n"));
    }

    @Test
    void roomsCsvProducesAttachment() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        ResponseEntity<String> response = controller.roomsCsv();

        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("rooms.csv"));
        assertEquals("roomNumber,roomType,fee,meetings\n", response.getBody());
    }

    @Test
    void usersCsvProducesAttachment() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        ResponseEntity<String> response = controller.usersCsv();

        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("users.csv"));
        assertEquals("userID,firstName,lastName,userEmail,role,displayName,birthDate\n",
                response.getBody());
    }
}
