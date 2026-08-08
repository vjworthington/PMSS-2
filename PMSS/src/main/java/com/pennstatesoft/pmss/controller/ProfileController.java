package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());

        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/profile-edit")
    public String editProfile(Model model, Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());

        model.addAttribute("user", user);

        return "profile-edit";
    }

    @PostMapping("/profile-edit")
    public String updateProfile(
            @RequestParam String displayName,
            @RequestParam String birthDate,
            @RequestParam(required = false) MultipartFile image,
            Authentication authentication) {

        String currentEmail = authentication.getName();

        userService.updateProfile(
                currentEmail,
                displayName,
                birthDate,
                image
        );

        return "redirect:/profile";
    }
}