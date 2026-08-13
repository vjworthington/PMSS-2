package com.pennstatesoft.pmss.service;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.UserRepository;
import com.pennstatesoft.pmss.security.SecurityLogger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final UserService service = new UserService(userRepository, securityLogger);

    private Client user() {
        return new Client(1, "user@pennstatesoft.com", "hash", "Last", "First",
                "CLIENT", "2000-01-01", "First L.", null, 0, null, null);
    }

    private static byte[] image(String format, int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    // ---- simple delegators ----

    @Test
    void findByEmailDelegates() {
        User expected = user();
        when(userRepository.findByEmail("user@pennstatesoft.com")).thenReturn(expected);
        assertSame(expected, service.findByEmail("user@pennstatesoft.com"));
    }

    @Test
    void resetLoginFailuresDelegates() {
        service.resetLoginFailures("user@pennstatesoft.com");
        verify(userRepository).resetLoginFailures("user@pennstatesoft.com");
    }

    // ---- loadUserByUsername ----

    @Test
    void loadUserThrowsWhenRepositoryFails() {
        when(userRepository.findByEmail("missing@pennstatesoft.com"))
                .thenThrow(new RuntimeException("no row"));

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@pennstatesoft.com"));
    }

    @Test
    void loadUserReturnsDetailsForUnlockedUser() {
        when(userRepository.findByEmail("user@pennstatesoft.com")).thenReturn(user());

        UserDetails details = service.loadUserByUsername("user@pennstatesoft.com");

        assertEquals("user@pennstatesoft.com", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT")));
    }

    @Test
    void loadUserThrowsLockedWhenLockStillActive() {
        Client locked = user();
        locked.setLockedTimeTo(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("user@pennstatesoft.com")).thenReturn(locked);

        assertThrows(LockedException.class,
                () -> service.loadUserByUsername("user@pennstatesoft.com"));
    }

    @Test
    void loadUserResetsFailuresWhenLockExpired() {
        Client expired = user();
        expired.setLockedTimeTo(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("user@pennstatesoft.com")).thenReturn(expired);

        UserDetails details = service.loadUserByUsername("user@pennstatesoft.com");

        assertEquals("user@pennstatesoft.com", details.getUsername());
        verify(userRepository).resetLoginFailures("user@pennstatesoft.com");
    }

    // ---- updateProfile ----

    @Test
    void updateProfileWithoutImageWhenNoFile() {
        service.updateProfile("user@pennstatesoft.com", "Name", "1999-01-01", null);

        verify(userRepository).updateProfileWithoutImage("user@pennstatesoft.com", "Name", "1999-01-01");
        verify(userRepository, never()).updateProfile(any(), any(), any(), any());
    }

    @Test
    void updateProfileWithoutImageWhenFileEmpty() {
        MockMultipartFile empty = new MockMultipartFile("image", "e.jpg", "image/jpeg", new byte[0]);

        service.updateProfile("user@pennstatesoft.com", "Name", "1999-01-01", empty);

        verify(userRepository).updateProfileWithoutImage("user@pennstatesoft.com", "Name", "1999-01-01");
    }

    @Test
    void updateProfileStoresValidImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "p.png", "image/png", image("png", 600, 600));

        service.updateProfile("user@pennstatesoft.com", "Name", "1999-01-01", file);

        verify(userRepository).updateProfile(eq("user@pennstatesoft.com"), eq("Name"),
                eq("1999-01-01"), any(byte[].class));
    }

    @Test
    void updateProfileRejectsUnsupportedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "p.gif", "image/gif", image("png", 600, 600));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile("user@pennstatesoft.com", "Name", "1999-01-01", file));
    }

    @Test
    void updateProfileRejectsOutOfRangeDimensions() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "p.png", "image/png", image("png", 100, 100));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile("user@pennstatesoft.com", "Name", "1999-01-01", file));
    }

    @Test
    void updateProfileRejectsNonImageBytes() {
        MockMultipartFile file = new MockMultipartFile("image", "p.jpg", "image/jpeg",
                new byte[]{1, 2, 3, 4});

        assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile("user@pennstatesoft.com", "Name", "1999-01-01", file));
    }

    // ---- recordFailedLogin ----

    @Test
    void recordFailedLoginDoesNothingWhenUserMissing() {
        when(userRepository.findByEmail("gone@pennstatesoft.com")).thenReturn(null);

        service.recordFailedLogin("gone@pennstatesoft.com");

        verify(userRepository, never()).updateLoginFailure(anyLong(), anyInt(), any(), any());
    }

    @Test
    void recordFailedLoginFirstFailureCountsOne() {
        when(userRepository.findByEmail("user@pennstatesoft.com")).thenReturn(user());

        service.recordFailedLogin("user@pennstatesoft.com");

        verify(userRepository).updateLoginFailure(eq(1L), eq(1), any(), isNull());
        verify(securityLogger, never()).accountLocked(any());
    }

    @Test
    void recordFailedLoginThirdFailureLocksAccount() {
        Client twoFails = user();
        twoFails.setFailedAttempts(2);
        twoFails.setLastFailedLogin(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("user@pennstatesoft.com")).thenReturn(twoFails);

        service.recordFailedLogin("user@pennstatesoft.com");

        // 3rd failure within the window -> lock timestamp supplied, account-locked logged.
        verify(userRepository).updateLoginFailure(eq(1L), eq(3), any(), any(LocalDateTime.class));
        verify(securityLogger).accountLocked("user@pennstatesoft.com");
    }

    @Test
    void recordFailedLoginResetsWindowAfterThirtyMinutes() {
        Client staleFails = user();
        staleFails.setFailedAttempts(2);
        staleFails.setLastFailedLogin(LocalDateTime.now().minusMinutes(31));
        when(userRepository.findByEmail("user@pennstatesoft.com")).thenReturn(staleFails);

        service.recordFailedLogin("user@pennstatesoft.com");

        // Old window expired -> counter restarts at 1, no lock.
        verify(userRepository).updateLoginFailure(eq(1L), eq(1), any(), isNull());
        verify(securityLogger, never()).accountLocked(any());
    }
}
