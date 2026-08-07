package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Complaint;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.ComplaintRowMapper;
import com.pennstatesoft.pmss.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/complaints")
public class ComplaintController {
    private static final List<String> COMPLAINT_OPTIONS = List.of(
            "Room Condition",
            "Billing",
            "Meeting Scheduling",
            "Staff Conduct",
            "Other"
    );

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final ComplaintRowMapper complaintRowMapper;

    public ComplaintController(JdbcTemplate jdbcTemplate,
                               UserService userService,
                               ComplaintRowMapper complaintRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.complaintRowMapper = complaintRowMapper;
    }

    @GetMapping
    public String displayComplaintForm(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());

        if ("ADMINISTRATOR".equals(user.getRole())) {
            return "redirect:/complaints/list";
        }

        model.addAttribute("user", user);
        model.addAttribute("meetings", findUserMeetings(user.getUserID()));
        model.addAttribute("complaintOptions", COMPLAINT_OPTIONS);
        model.addAttribute("complaints", findComplaintsByUser(user.getUserID()));

        return "complaints";
    }

    @PostMapping
    public String submitComplaint(@RequestParam(required = false, defaultValue = "0") int meetingID,
                                  @RequestParam String complaintOption,
                                  @RequestParam String summary,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());

        if (!validateMeetingSelection(meetingID, user.getUserID())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please select a meeting you attended, or leave it blank.");
            return "redirect:/complaints";
        }

        if (!validateComplaintOption(complaintOption)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please select a valid complaint category.");
            return "redirect:/complaints";
        }

        if (summary == null || summary.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please describe the issue before submitting.");
            return "redirect:/complaints";
        }

        Complaint complaint = new Complaint(user.getUserID(), meetingID, complaintOption, summary);
        insertComplaint(complaint);

        redirectAttributes.addFlashAttribute("successMessage",
                "Your complaint has been submitted.");

        return "redirect:/complaints";
    }

    @GetMapping("/list")
    public String retrieveComplaintList(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());

        if (!"ADMINISTRATOR".equals(user.getRole())) {
            return "redirect:/complaints";
        }

        model.addAttribute("user", user);
        model.addAttribute("complaints", findAllComplaints());

        return "complaints";
    }

    @GetMapping("/{complaintID}")
    public String retrieveComplaintDetails(@PathVariable int complaintID,
                                           Model model,
                                           Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());
        if (!"ADMINISTRATOR".equals(user.getRole())) {
            return "redirect:/complaints";
        }

        Complaint complaint = findComplaintById(complaintID);

        if (complaint == null) {
            return "redirect:/complaints/list";
        }

        model.addAttribute("user", user);
        model.addAttribute("complaint", complaint);

        return "complaint-edit";
    }

    @PostMapping("/{complaintID}/respond")
    public String submitResponse(@PathVariable int complaintID,
                                 @RequestParam String adminResponse,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());

        if (!"ADMINISTRATOR".equals(user.getRole())) {
            return "redirect:/complaints";
        }

        if (adminResponse == null || adminResponse.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "A response is required.");
            return "redirect:/complaints/" + complaintID;
        }

        Complaint complaint = findComplaintById(complaintID);

        if (complaint == null) {
            return "redirect:/complaints/list";
        }

        complaint.setAdminResponse(adminResponse);
        updateComplaintResponse(complaint);

        redirectAttributes.addFlashAttribute("successMessage",
                "Response submitted. Complaint marked as resolved.");

        return "redirect:/complaints/list";
    }

    @PostMapping("/{complaintID}/status")
    public String updateComplaintStatus(@PathVariable int complaintID,
                                        @RequestParam String newStatus,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {

        User user = userService.findByEmail(authentication.getName());
        if (!"ADMINISTRATOR".equals(user.getRole())) {
            return "redirect:/complaints";
        }

        Complaint complaint = findComplaintById(complaintID);
        if (complaint == null) {
            return "redirect:/complaints/list";
        }

        complaint.updateStatus(newStatus);
        updateComplaintStatusInDb(complaint);
        redirectAttributes.addFlashAttribute("successMessage", "Complaint status updated.");

        return "redirect:/complaints/" + complaintID;
    }

    private boolean validateMeetingSelection(int meetingID, int userID) {
        if (meetingID == 0) {
            return true;
        }

        String sql = """
        SELECT COUNT(*)
        FROM Meetings m
        LEFT JOIN MeetingAttendees a ON m.meetingID = a.meetingID
        WHERE m.meetingID = ?
        AND (m.userID = ? OR a.userID = ?)
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, meetingID, userID, userID);

        return count != null && count > 0;
    }

    private boolean validateComplaintOption(String complaintOption) {
        return complaintOption != null && COMPLAINT_OPTIONS.contains(complaintOption);
    }

    private List<Map<String, Object>> findUserMeetings(int userID) {
        String sql = """
        SELECT DISTINCT m.meetingID, m.meetingName, m.meetingDate
        FROM Meetings m
        LEFT JOIN MeetingAttendees a ON m.meetingID = a.meetingID
        WHERE m.userID = ? OR a.userID = ?
        ORDER BY m.meetingDate DESC
        """;
        return jdbcTemplate.queryForList(sql, userID, userID);
    }

    private void insertComplaint(Complaint complaint) {
        String sql = """
        INSERT INTO Complaints (meetingID, userID, complaintOption, summary, status)
        VALUES (?, ?, ?, ?, ?)
        """;
        Object meetingID = complaint.getMeetingID() == 0 ? null : complaint.getMeetingID();
        jdbcTemplate.update(sql,
                meetingID,
                complaint.getUserID(),
                complaint.getComplaintOption(),
                complaint.getSummary(),
                complaint.getStatus());
    }

    private List<Complaint> findComplaintsByUser(int userID) {
        String sql = "SELECT * FROM Complaints WHERE userID = ? ORDER BY dateFiled DESC";
        return jdbcTemplate.query(sql, complaintRowMapper, userID);
    }

    private List<Complaint> findAllComplaints() {
        String sql = "SELECT * FROM Complaints ORDER BY dateFiled DESC";
        return jdbcTemplate.query(sql, complaintRowMapper);
    }

    private Complaint findComplaintById(int complaintID) {
        String sql = "SELECT * FROM Complaints WHERE complaintID = ?";
        List<Complaint> results = jdbcTemplate.query(sql, complaintRowMapper, complaintID);
        return results.isEmpty() ? null : results.get(0);
    }

    private void updateComplaintResponse(Complaint complaint) {
        String sql = "UPDATE Complaints SET adminResponse = ?, status = ? WHERE complaintID = ?";
        jdbcTemplate.update(sql, complaint.getAdminResponse(), complaint.getStatus(), complaint.getComplaintID());
    }

    private void updateComplaintStatusInDb(Complaint complaint) {
        String sql = "UPDATE Complaints SET status = ? WHERE complaintID = ?";
        jdbcTemplate.update(sql, complaint.getStatus(), complaint.getComplaintID());
    }
}
