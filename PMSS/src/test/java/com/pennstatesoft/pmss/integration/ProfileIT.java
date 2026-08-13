package com.pennstatesoft.pmss.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Profile viewing/editing (/profile, /profile-edit, /profile-image), including
 * the image-upload validation (JPG/PNG only, 500–1000px each side) enforced in
 * UserService, and round-tripping a stored image back out through /profile-image.
 */
class ProfileIT extends AbstractIntegrationTest {

    private org.springframework.test.web.servlet.request.RequestPostProcessor asClient() {
        return user(CLIENT_EMAIL).roles("CLIENT");
    }

    private byte[] jpeg(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    private String displayName(int userID) {
        return jdbc.queryForObject("SELECT displayName FROM Users WHERE userID = ?", String.class, userID);
    }

    private byte[] storedImage(int userID) {
        return jdbc.queryForObject("SELECT profileImage FROM Users WHERE userID = ?", byte[].class, userID);
    }

    // ---- view ----

    @Test
    void clientViewsProfile() throws Exception {
        mockMvc.perform(get("/profile").with(asClient()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }

    @Test
    void clientOpensProfileEditForm() throws Exception {
        mockMvc.perform(get("/profile-edit").with(asClient()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"));
    }

    // ---- update ----

    @Test
    void updateProfileWithoutImagePersistsFields() throws Exception {
        mockMvc.perform(multipart("/profile-edit").with(asClient()).with(csrf())
                        .param("displayName", "Casey the Client")
                        .param("birthDate", "1991-02-03"))
                .andExpect(redirectedUrl("/profile"));

        assertEquals("Casey the Client", displayName(clientId));
        assertEquals("1991-02-03", jdbc.queryForObject(
                "SELECT birthDate FROM Users WHERE userID = ?", String.class, clientId));
        // No image was uploaded, so the stored image stays empty.
        assertNull(storedImage(clientId));
    }

    @Test
    void updateProfileWithValidImageStoresBytes() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "avatar.jpg", "image/jpeg", jpeg(600, 600));

        mockMvc.perform(multipart("/profile-edit").file(image).with(asClient()).with(csrf())
                        .param("displayName", "Casey the Client")
                        .param("birthDate", "1991-02-03"))
                .andExpect(redirectedUrl("/profile"));

        byte[] stored = storedImage(clientId);
        assertNotNull(stored);
        assertTrue(stored.length > 0);

        // The stored image is served back out through /profile-image.
        byte[] served = mockMvc.perform(get("/profile-image").with(asClient()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(stored.length, served.length);
    }

    @Test
    void updateProfileRejectsUndersizedImage() throws Exception {
        MockMultipartFile tooSmall = new MockMultipartFile(
                "image", "tiny.jpg", "image/jpeg", jpeg(100, 100));

        // UserService throws IllegalArgumentException -> GlobalExceptionHandler -> 400.
        mockMvc.perform(multipart("/profile-edit").file(tooSmall).with(asClient()).with(csrf())
                        .param("displayName", "Casey the Client")
                        .param("birthDate", "1991-02-03"))
                .andExpect(status().isBadRequest());

        assertNull(storedImage(clientId));
    }

    @Test
    void updateProfileRejectsNonImageDisguisedAsJpeg() throws Exception {
        MockMultipartFile bogus = new MockMultipartFile(
                "image", "notreally.jpg", "image/jpeg", "this is not an image".getBytes());

        mockMvc.perform(multipart("/profile-edit").file(bogus).with(asClient()).with(csrf())
                        .param("displayName", "Casey the Client")
                        .param("birthDate", "1991-02-03"))
                .andExpect(status().isBadRequest());

        assertNull(storedImage(clientId));
    }
}
