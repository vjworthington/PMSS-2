package com.pennstatesoft.pmss.service;

import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        try {
            user = userRepository.findByEmail(email);
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found");
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
                    throw new IllegalArgumentException(
                            "Invalid image file.");
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

}