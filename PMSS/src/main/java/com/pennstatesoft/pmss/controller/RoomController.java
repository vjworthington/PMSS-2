package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.security.SecurityLogger;
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
    private final SecurityLogger securityLogger;
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String ADMIN_ROOMS_URL = "redirect:/admin/rooms";
    private static final String SPECIAL_STRING = "SPECIAL";
    private static final String ROOM_STRING = "Room ";


    public RoomController(JdbcTemplate jdbcTemplate, UserService userService, SecurityLogger securityLogger) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.securityLogger = securityLogger;
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
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {

        if (roomNumber == null || roomNumber <= 0) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Enter a valid room number.");
        }

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Rooms WHERE roomNumber = ?", Integer.class, roomNumber);
        if (exists != null && exists > 0) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                    ROOM_STRING + roomNumber + " already exists.");
        }

        String type = SPECIAL_STRING.equalsIgnoreCase(roomType) ? SPECIAL_STRING: "REGULAR";
        double roomFee;

        if (SPECIAL_STRING.equals(type)) {
            roomFee = fee == null ? 100.00 : fee;
        } else {
            roomFee = 0.0;
        }

        jdbcTemplate.update(
                "INSERT INTO Rooms (roomNumber, isOccupied, fee, roomType) VALUES (?, 0, ?, ?)",
                roomNumber, roomFee, type);

        securityLogger.roomCreated(authentication.getName(), roomNumber);
        redirectAttributes.addFlashAttribute("successMessage",
                ROOM_STRING + roomNumber + " (" + type + ") added.");
        return ADMIN_ROOMS_URL;
    }

    @PostMapping("/{roomNumber}/delete")
    public String deleteRoom(@PathVariable("roomNumber") int roomNumber,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        // Block deletion of a room that still has meetings.
        Integer meetings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Meetings WHERE roomNumber = ?", Integer.class, roomNumber);
        if (meetings != null && meetings > 0) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                    ROOM_STRING + roomNumber + " has " + meetings
                            + " meeting(s) and can't be deleted. Remove those meetings first.");
        }

        jdbcTemplate.update("DELETE FROM Rooms WHERE roomNumber = ?", roomNumber);
        securityLogger.roomDeleted(authentication.getName(), roomNumber);
        redirectAttributes.addFlashAttribute("successMessage", ROOM_STRING + roomNumber + " deleted.");
        return ADMIN_ROOMS_URL;
    }

    private List<Map<String, Object>> findAllRooms() {
        return jdbcTemplate.queryForList(
                "SELECT roomNumber, roomType, fee FROM Rooms ORDER BY roomNumber");
    }
}
