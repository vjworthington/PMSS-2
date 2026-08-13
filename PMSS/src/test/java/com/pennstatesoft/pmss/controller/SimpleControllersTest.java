package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests for the thin controllers that only wire a service call to a view name. */
class SimpleControllersTest {

    private User user(String email) {
        return new Client(1, email, "h", "L", "F", "CLIENT", "2000-01-01", "F L",
                new byte[]{1, 2, 3}, 0, null, null);
    }

    private Authentication authFor(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    @Test
    void homeControllerRedirectsToLogin() {
        assertEquals("redirect:/login", new HomeController().home());
    }

    @Test
    void loginControllerReturnsLoginView() {
        assertEquals("login", new LoginController().login());
    }

    @Test
    void adminLandingAddsUserAndReturnsView() {
        UserService userService = mock(UserService.class);
        User u = user("admin@x.com");
        when(userService.findByEmail("admin@x.com")).thenReturn(u);
        Model model = new ExtendedModelMap();

        String view = new AdminLandingController(userService).landing(model, authFor("admin@x.com"));

        assertEquals("admin/landing", view);
        assertSame(u, model.getAttribute("user"));
    }

    @Test
    void clientLandingAddsUserAndReturnsView() {
        UserService userService = mock(UserService.class);
        User u = user("client@x.com");
        when(userService.findByEmail("client@x.com")).thenReturn(u);
        Model model = new ExtendedModelMap();

        String view = new ClientLandingController(userService).landing(model, authFor("client@x.com"));

        assertEquals("client/landing", view);
        assertSame(u, model.getAttribute("user"));
    }

    @Test
    void profileShowsProfileView() {
        UserService userService = mock(UserService.class);
        User u = user("a@x.com");
        when(userService.findByEmail("a@x.com")).thenReturn(u);
        Model model = new ExtendedModelMap();

        String view = new ProfileController(userService, mock(SecurityLogger.class))
                .profile(model, authFor("a@x.com"));

        assertEquals("profile", view);
        assertSame(u, model.getAttribute("user"));
    }

    @Test
    void editProfileShowsEditView() {
        UserService userService = mock(UserService.class);
        when(userService.findByEmail("a@x.com")).thenReturn(user("a@x.com"));
        Model model = new ExtendedModelMap();

        String view = new ProfileController(userService, mock(SecurityLogger.class))
                .editProfile(model, authFor("a@x.com"));

        assertEquals("profile-edit", view);
    }

    @Test
    void updateProfileDelegatesAndRedirects() {
        UserService userService = mock(UserService.class);
        SecurityLogger securityLogger = mock(SecurityLogger.class);
        MultipartFile image = mock(MultipartFile.class);
        ProfileController controller = new ProfileController(userService, securityLogger);

        String view = controller.updateProfile("New Name", "1999-01-01", image, authFor("a@x.com"));

        assertEquals("redirect:/profile", view);
        verify(userService).updateProfile("a@x.com", "New Name", "1999-01-01", image);
        verify(securityLogger).profileChanged("a@x.com");
    }

    @Test
    void imageControllerReturnsProfileImageAsJpeg() {
        UserService userService = mock(UserService.class);
        when(userService.findByEmail("a@x.com")).thenReturn(user("a@x.com"));

        ResponseEntity<byte[]> response = new ImageController(userService).profileImage(authFor("a@x.com"));

        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
    }
}
