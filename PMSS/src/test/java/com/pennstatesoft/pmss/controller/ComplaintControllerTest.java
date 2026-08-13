package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.Complaint;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.ComplaintRowMapper;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplaintControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserService userService = mock(UserService.class);
    private final ComplaintRowMapper complaintRowMapper = mock(ComplaintRowMapper.class);
    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final ComplaintController controller =
            new ComplaintController(jdbc, userService, complaintRowMapper, securityLogger);

    private User userWithRole(String role) {
        return new Client(1, "u@pennstatesoft.com", "h", "L", "F", role,
                "2000-01-01", "F L", null, 0, null, null);
    }

    private Authentication authAs(String role) {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("u@pennstatesoft.com");
        when(userService.findByEmail("u@pennstatesoft.com")).thenReturn(userWithRole(role));
        return a;
    }

    private String error(RedirectAttributesModelMap ra) {
        return (String) ra.getFlashAttributes().get("errorMessage");
    }

    @Test
    void displayFormPopulatesModel() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.displayComplaintForm(model, authAs("CLIENT"));

        assertEquals("complaints", view);
        assertEquals(true, model.getAttribute("fileMode"));
    }

    @Test
    void submitComplaintRejectsInvalidMeetingSelection() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitComplaint(55, "Billing", "text", authAs("CLIENT"), ra);

        assertEquals("redirect:/complaints", view);
        assertEquals("Please select a meeting you attended, or leave it blank.", error(ra));
    }

    @Test
    void submitComplaintRejectsUnknownOption() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitComplaint(0, "NotAnOption", "text", authAs("CLIENT"), ra);

        assertEquals("Please select a valid complaint category.", error(ra));
    }

    @Test
    void submitComplaintRejectsBlankSummary() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitComplaint(0, "Billing", "   ", authAs("CLIENT"), ra);

        assertEquals("Please describe the issue before submitting.", error(ra));
    }

    @Test
    void submitComplaintInsertsWhenValid() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitComplaint(0, "Billing", "Double charged", authAs("CLIENT"), ra);

        assertEquals("redirect:/complaints", view);
        assertEquals("Your complaint has been submitted.",
                ra.getFlashAttributes().get("successMessage"));
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any());
        verify(securityLogger).complaintFiled("u@pennstatesoft.com", 0);
    }

    @Test
    void complaintListRedirectsForNonAdmin() {
        Model model = new ExtendedModelMap();

        String view = controller.retrieveComplaintList(model, authAs("CLIENT"));

        assertEquals("redirect:/complaints", view);
    }

    @Test
    void complaintListShownForAdmin() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.retrieveComplaintList(model, authAs("ADMINISTRATOR"));

        assertEquals("complaints", view);
        assertEquals(false, model.getAttribute("fileMode"));
    }

    @Test
    void complaintDetailsRedirectsWhenMissing() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.retrieveComplaintDetails(7, model, authAs("ADMINISTRATOR"));

        assertEquals("redirect:/complaints/list", view);
    }

    @Test
    void complaintDetailsShownWhenFound() {
        Complaint complaint = new Complaint(1, 0, "Billing", "text");
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of(complaint));
        Model model = new ExtendedModelMap();

        String view = controller.retrieveComplaintDetails(7, model, authAs("ADMINISTRATOR"));

        assertEquals("complaint-edit", view);
    }

    @Test
    void submitResponseRejectsBlankResponse() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitResponse(7, "  ", authAs("ADMINISTRATOR"), ra);

        assertEquals("redirect:/complaints/7", view);
        assertEquals("A response is required.", error(ra));
    }

    @Test
    void submitResponseMarksResolved() {
        Complaint complaint = new Complaint(1, 0, "Billing", "text");
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of(complaint));
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.submitResponse(7, "Resolved for you", authAs("ADMINISTRATOR"), ra);

        assertEquals("redirect:/complaints/list", view);
        assertEquals("RESOLVED", complaint.getStatus());
        verify(jdbc).update(anyString(), eq("Resolved for you"), eq("RESOLVED"), any());
        verify(securityLogger).complaintResolved("u@pennstatesoft.com", 7);
    }

    @Test
    void updateStatusRedirectsForNonAdmin() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.updateComplaintStatus(7, "IN_PROGRESS", authAs("CLIENT"), ra);

        assertEquals("redirect:/complaints", view);
        verify(jdbc, never()).update(anyString(), any(), any());
    }

    @Test
    void updateStatusPersistsForAdmin() {
        Complaint complaint = new Complaint(1, 0, "Billing", "text");
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of(complaint));
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.updateComplaintStatus(7, "IN_PROGRESS", authAs("ADMINISTRATOR"), ra);

        assertEquals("redirect:/complaints/7", view);
        assertEquals("IN_PROGRESS", complaint.getStatus());
        verify(jdbc).update(anyString(), eq("IN_PROGRESS"), any());
    }
}
