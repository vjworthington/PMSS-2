package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.service.UserService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Administrator room management 
 */
@Controller
@RequestMapping("/admin/rooms")
public class RoomController {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    public RoomController(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    @GetMapping
    public String listRooms(Model model, Authentication authentication) {
        model.addAttribute("user", userService.findByEmail(authentication.getName()));
        model.addAttribute("rooms", findAllRooms());
        return "admin/rooms";
    }

    @PostMapping("/add")
    public String addRoom(@RequestParam(name = "roomNumber", required = false) Integer roomNumber,
                          @RequestParam(name = "roomType", required = false) String roomType,
                          @RequestParam(name = "fee", required = false) Double fee,
                          RedirectAttributes redirectAttributes) {

        if (roomNumber == null || roomNumber <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enter a valid room number.");
            return "redirect:/admin/rooms";
        }

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Rooms WHERE roomNumber = ?", Integer.class, roomNumber);
        if (exists != null && exists > 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Room " + roomNumber + " already exists.");
            return "redirect:/admin/rooms";
        }

        String type = "SPECIAL".equalsIgnoreCase(roomType) ? "SPECIAL" : "REGULAR";
        double roomFee = "SPECIAL".equals(type)
                ? (fee == null ? 100.00 : fee)
                : 0.0;

        jdbcTemplate.update(
                "INSERT INTO Rooms (roomNumber, isOccupied, fee, roomType) VALUES (?, 0, ?, ?)",
                roomNumber, roomFee, type);

        redirectAttributes.addFlashAttribute("successMessage",
                "Room " + roomNumber + " (" + type + ") added.");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/{roomNumber}/delete")
    public String deleteRoom(@PathVariable("roomNumber") int roomNumber,
                             RedirectAttributes redirectAttributes) {

        // Block deletion of a room that still has meetings.
        Integer meetings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Meetings WHERE roomNumber = ?", Integer.class, roomNumber);
        if (meetings != null && meetings > 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Room " + roomNumber + " has " + meetings
                            + " meeting(s) and can't be deleted. Remove those meetings first.");
            return "redirect:/admin/rooms";
        }

        jdbcTemplate.update("DELETE FROM Rooms WHERE roomNumber = ?", roomNumber);
        redirectAttributes.addFlashAttribute("successMessage", "Room " + roomNumber + " deleted.");
        return "redirect:/admin/rooms";
    }

    private List<Map<String, Object>> findAllRooms() {
        return jdbcTemplate.queryForList(
                "SELECT roomNumber, roomType, fee FROM Rooms ORDER BY roomNumber");
    }
}
