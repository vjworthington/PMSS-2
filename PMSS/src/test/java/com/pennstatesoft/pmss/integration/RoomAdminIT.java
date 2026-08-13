package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Admin room management over HTTP (/admin/rooms): add/delete with validation,
 * the "can't delete a room that still has meetings" guard, and the
 * ADMINISTRATOR-only authorization on the whole area.
 */
class RoomAdminIT extends AbstractIntegrationTest {

    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        return user(ADMIN_EMAIL).roles("ADMINISTRATOR");
    }

    private int roomCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM Rooms", Integer.class);
    }

    private boolean roomExists(int roomNumber) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM Rooms WHERE roomNumber = ?", Integer.class, roomNumber) > 0;
    }

    // ---- add ----

    @Test
    void adminAddsRegularRoomWithZeroFee() throws Exception {
        mockMvc.perform(post("/admin/rooms/add").with(asAdmin()).with(csrf())
                        .param("roomNumber", "901")
                        .param("roomType", "REGULAR"))
                .andExpect(redirectedUrl("/admin/rooms"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(0.0, jdbc.queryForObject(
                "SELECT fee FROM Rooms WHERE roomNumber = ?", Double.class, 901));
        assertEquals("REGULAR", jdbc.queryForObject(
                "SELECT roomType FROM Rooms WHERE roomNumber = ?", String.class, 901));
    }

    @Test
    void adminAddsSpecialRoomWithProvidedFee() throws Exception {
        mockMvc.perform(post("/admin/rooms/add").with(asAdmin()).with(csrf())
                        .param("roomNumber", "902")
                        .param("roomType", "SPECIAL")
                        .param("fee", "250.0"))
                .andExpect(redirectedUrl("/admin/rooms"));

        assertEquals(250.0, jdbc.queryForObject(
                "SELECT fee FROM Rooms WHERE roomNumber = ?", Double.class, 902));
        assertEquals("SPECIAL", jdbc.queryForObject(
                "SELECT roomType FROM Rooms WHERE roomNumber = ?", String.class, 902));
    }

    @Test
    void addRoomRejectsInvalidNumberWithoutInserting() throws Exception {
        int before = roomCount();

        mockMvc.perform(post("/admin/rooms/add").with(asAdmin()).with(csrf())
                        .param("roomNumber", "0")
                        .param("roomType", "REGULAR"))
                .andExpect(redirectedUrl("/admin/rooms"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(before, roomCount());
    }

    @Test
    void addRoomRejectsDuplicateWithoutInserting() throws Exception {
        int before = roomCount();

        mockMvc.perform(post("/admin/rooms/add").with(asAdmin()).with(csrf())
                        .param("roomNumber", String.valueOf(regularRoom))
                        .param("roomType", "REGULAR"))
                .andExpect(redirectedUrl("/admin/rooms"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(before, roomCount());
    }

    // ---- delete ----

    @Test
    void adminDeletesEmptyRoom() throws Exception {
        // specialRoom has no meetings.
        mockMvc.perform(post("/admin/rooms/" + specialRoom + "/delete").with(asAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/rooms"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(false, roomExists(specialRoom));
    }

    @Test
    void deleteRoomBlockedWhenMeetingsExist() throws Exception {
        seedMeeting("Occupies Room", "2026-09-01", "09:00", "10:00", clientId, regularRoom);

        mockMvc.perform(post("/admin/rooms/" + regularRoom + "/delete").with(asAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/rooms"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertEquals(true, roomExists(regularRoom));
    }

    // ---- authorization ----

    @Test
    void adminCanViewRoomList() throws Exception {
        mockMvc.perform(get("/admin/rooms").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rooms"));
    }

    @Test
    void clientCannotAccessRoomAdmin() throws Exception {
        mockMvc.perform(get("/admin/rooms").with(user(CLIENT_EMAIL).roles("CLIENT")))
                .andExpect(status().isForbidden());
    }
}
