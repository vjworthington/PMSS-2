package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ImageController {

    private final UserService userService;

    public ImageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile-image")
    public ResponseEntity<byte[]> profileImage(Authentication authentication) {

        byte[] image = userService
                .findByEmail(authentication.getName())
                .getProfileImage();

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }
}