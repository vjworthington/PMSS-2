package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    private final UserService userService;

    public LandingController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/landing")
    public String landing(Model model, Authentication authentication) {

        User user = userService.findByEmail(authentication.getName());

        model.addAttribute("user", user);

        return "landing";
    }
}

