/** NOTE:
 * This ensures localhost:8080/ goes to login page not a static page
 */

package com.pennstatesoft.pmss.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}