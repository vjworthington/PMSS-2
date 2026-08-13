package com.pennstatesoft.pmss.service;

import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.UserRepository;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.pennstatesoft.pmss.security.SecurityLogger;

import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SecurityLogger securityLogger;

    public UserService(UserRepository userRepository, SecurityLogger securityLogger) {

        this.userRepository = userRepository;
        this.securityLogger = securityLogger;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user;

        try {
            user = userRepository.findByEmail(email);
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found");
        }

        if (user.getLockedTimeTo() != null) {

            if (LocalDateTime.now(ZoneId.systemDefault()).isBefore(user.getLockedTimeTo())) {

                throw new LockedException("Account is temporarily locked");

            } else {

                // 15-minute lock has expired
                userRepository.resetLoginFailures(user.getUserEmail());

                user.setFailedAttempts(0);
                user.setLastFailedLogin(null);
                user.setLockedTimeTo(null);
            }
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void updateProfile(
            String email,
            String displayName,
            String birthDate,
            MultipartFile image) {

        if (image != null && !image.isEmpty()) {

            byte[] imageBytes;

            try {
                BufferedImage bufferedImage = ImageIO.read(image.getInputStream());

                if (bufferedImage == null) {
                    throw new IllegalArgumentException("Invalid image file.");
                }

                String contentType = image.getContentType();

                if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
                    throw new IllegalArgumentException("Image must be JPG or PNG.");
                }

                int width = bufferedImage.getWidth();
                int height = bufferedImage.getHeight();

                if (width < 500 || width > 1000 || height < 500 || height > 1000) {
                    throw new IllegalArgumentException("Image width and height must each be between 500 and 1000 pixels.");
                }

                imageBytes = image.getBytes();

                // Valid image → update image
                userRepository.updateProfile(
                        email,
                        displayName,
                        birthDate,
                        imageBytes
                );

            } catch (IOException e) {
                throw new RuntimeException("Could not process image.", e);
            }

        } else {
            // No new image → keep existing image
            userRepository.updateProfileWithoutImage(
                    email,
                    displayName,
                    birthDate
            );
        }
    }

    public void recordFailedLogin(String email) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        int failedAttempts = user.getFailedAttempts();

        LocalDateTime lastFailedLogin = user.getLastFailedLogin();

        // If the previous failure was more than 30 minutes ago, start a new failure window.
        if (lastFailedLogin == null || lastFailedLogin.plusMinutes(30).isBefore(now)) {
            failedAttempts = 0;
        }

        failedAttempts++;

        //Third failed attempt within 30 minutes
        if (failedAttempts >= 3) {
            LocalDateTime lockedTimeTo = now.plusMinutes(15);

            userRepository.updateLoginFailure(
                    user.getUserID(),
                    failedAttempts,
                    now,
                    lockedTimeTo
            );

            securityLogger.accountLocked(email);

        } else {

            userRepository.updateLoginFailure(
                    user.getUserID(),
                    failedAttempts,
                    now,
                    null
            );
        }
    }

    public void resetLoginFailures(String email) {
        userRepository.resetLoginFailures(email);
    }
}