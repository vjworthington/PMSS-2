package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

/**
 * Controller implementation for the Register module.
 *
 * Implements RegisterAdminControllerIF (which extends RegisterControllerIF),
 * so it provides every account-creation method.
 */
@Controller
public class RegisterController implements RegisterAdminControllerIF {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    private String pendingFirstName;
    private String pendingLastName;
    private String pendingEmail;
    private String pendingPassword;
    private Date pendingBirthDate;

    private final SecurityLogger securityLogger;

    public RegisterController(JdbcTemplate jdbcTemplate,
                             PasswordEncoder passwordEncoder,
                             UserService userService,
                             SecurityLogger securityLogger) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.securityLogger = securityLogger;
    }

    @GetMapping("/register")
    public String displayForm() {
        return "register";
    }

    @PostMapping("/register")
    public String submitForm(@RequestParam(name = "firstName") String firstName,
                             @RequestParam(name = "lastName") String lastName,
                             @RequestParam(name = "email") String email,
                             @RequestParam(name = "password") String password,
                             @RequestParam(name = "birthDate") String birthDate,
                             RedirectAttributes redirectAttributes) {

        String error = validateForm(firstName, lastName, email, password, birthDate);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/register";
        }

        stagePending(firstName, lastName);

        boolean created = registerAccount(email.trim(), password, parseDate(birthDate));
        if (!created) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "An account with that email already exists.");
            return "redirect:/register";
        }

        securityLogger.clientAccountCreated(email.trim());
        redirectAttributes.addFlashAttribute("successMessage",
            "Account created. You can now log in.");
        return "redirect:/login";
    }

    @GetMapping("/admin/register")
    public String displayAdminForm(Model model, Authentication authentication) {
        model.addAttribute("user", userService.findByEmail(authentication.getName()));
        return "admin/register";
    }

    @PostMapping("/admin/register")
    public String submitAdminForm(@RequestParam(name = "firstName") String firstName,
                                  @RequestParam(name = "lastName") String lastName,
                                  @RequestParam(name = "email") String email,
                                  @RequestParam(name = "password") String password,
                                  @RequestParam(name = "birthDate") String birthDate,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {

        String error = validateForm(firstName, lastName, email, password, birthDate);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/admin/register";
        }

        stagePending(firstName, lastName);
        this.pendingEmail = email.trim();
        this.pendingPassword = password;
        this.pendingBirthDate = parseDate(birthDate);

        if (checkEmailUnique(this.pendingEmail)) {
            createAdmin();
            securityLogger.adminAccountCreated(authentication.getName(), this.pendingEmail);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Administrator account created for " + this.pendingEmail + ".");
            return "redirect:/admin/register";
        }

        redirectAttributes.addFlashAttribute("errorMessage",
                "An account with that email already exists.");
        return "redirect:/admin/register";
    }

    @Override
    public boolean registerAccount(String email, String password, Date birthDate) {
        this.pendingEmail = email;
        this.pendingPassword = password;
        this.pendingBirthDate = birthDate;

        if (checkEmailUnique(email)) {
            createClient();
            return true;
        }
        return false;
    }

    @Override
    public boolean checkEmailUnique(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Users WHERE userEmail = ?", Integer.class, email);
        return count == 0;
    }

    @Override
    public void createClient() {
        insertUser("CLIENT");
    }

    @Override
    public void createAdmin() {
        insertUser("ADMINISTRATOR");
    }

    // Helper Methods

    private void insertUser(String role) {
        String sql = """
            INSERT INTO Users (userEmail, passwordHash, lastName, firstName, role, displayName, birthDate)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String displayName = (pendingFirstName + " " + pendingLastName).trim();

        jdbcTemplate.update(sql,
                pendingEmail,
                passwordEncoder.encode(pendingPassword),
                pendingLastName,
                pendingFirstName,
                role,
                displayName,
                pendingBirthDate == null ? null : pendingBirthDate.toString());
    }

    private void stagePending(String firstName, String lastName) {
        this.pendingFirstName = firstName.trim();
        this.pendingLastName = lastName.trim();
    }

    private String validateForm(String firstName,
                                String lastName,
                                String email,
                                String password,
                                String birthDate) {
        if (isBlank(firstName) || isBlank(lastName)) {
            return "First and last name are required.";
        }
        if (isBlank(email) || !email.contains("@")) {
            return "Please enter a valid email address.";
        }
        if (isBlank(password)) {
            return "Password is required.";
        }
        if (isBlank(birthDate)) {
            return "Birth date is required.";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Date parseDate(String value) {
        return java.sql.Date.valueOf(value.trim());
    }
}
