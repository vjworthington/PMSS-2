package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserService userService = mock(UserService.class);
    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final RoomController controller = new RoomController(jdbc, userService, securityLogger);

    private Authentication auth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@pennstatesoft.com");
        return auth;
    }

    private String error(RedirectAttributesModelMap ra) {
        return (String) ra.getFlashAttributes().get("errorMessage");
    }

    private String success(RedirectAttributesModelMap ra) {
        return (String) ra.getFlashAttributes().get("successMessage");
    }

    @Test
    void addRoomRejectsInvalidNumber() {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.addRoom(0, "REGULAR", null, auth(), ra);

        assertEquals("redirect:/admin/rooms", view);
        assertEquals("Enter a valid room number.", error(ra));
        verify(jdbc, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void addRoomRejectsDuplicate() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.addRoom(101, "REGULAR", null, auth(), ra);

        assertEquals("Room 101 already exists.", error(ra));
    }

    @Test
    void addRegularRoomInsertsWithZeroFee() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.addRoom(101, "REGULAR", null, auth(), ra);

        assertEquals("redirect:/admin/rooms", view);
        assertEquals("Room 101 (REGULAR) added.", success(ra));
        verify(jdbc).update(anyString(), eq(101), eq(0.0), eq("REGULAR"));
        verify(securityLogger).roomCreated("admin@pennstatesoft.com", 101);
    }

    @Test
    void addSpecialRoomDefaultsFeeToHundred() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        controller.addRoom(202, "special", null, auth(), ra);

        verify(jdbc).update(anyString(), eq(202), eq(100.00), eq("SPECIAL"));
    }

    @Test
    void addSpecialRoomUsesProvidedFee() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        controller.addRoom(202, "SPECIAL", 250.0, auth(), ra);

        verify(jdbc).update(anyString(), eq(202), eq(250.0), eq("SPECIAL"));
    }

    @Test
    void deleteRoomBlockedWhenMeetingsExist() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(3);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.deleteRoom(101, auth(), ra);

        assertEquals("redirect:/admin/rooms", view);
        assertEquals("Room 101 has 3 meeting(s) and can't be deleted. Remove those meetings first.",
                error(ra));
        verify(jdbc, never()).update(anyString(), eq(101));
    }

    @Test
    void deleteRoomRemovesWhenNoMeetings() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.deleteRoom(101, auth(), ra);

        assertEquals("Room 101 deleted.", success(ra));
        verify(jdbc).update(anyString(), eq(101));
        verify(securityLogger).roomDeleted("admin@pennstatesoft.com", 101);
    }
}
