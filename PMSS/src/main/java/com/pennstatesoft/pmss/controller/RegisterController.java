package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.util.regex.Pattern;

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
    private final SecurityLogger securityLogger;
    private static final String BIRTHDAY_PROMPT = "Please enter a valid birth date.";
    private static final String ADMIN_REGISTER_URL = "redirect:/admin/register";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String REGISTER_URL = "redirect:/register";


    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} .'-]+$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@pennstatesoft\\.com$");

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

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

        try {

            firstName = firstName == null ? "" : firstName.trim();
            lastName = lastName == null ? "" : lastName.trim();
            email = email == null ? "" : email.trim().toLowerCase();

            // Validate all input
            String error = validateForm(
                    firstName,
                    lastName,
                    email,
                    password,
                    birthDate
            );

            if (error != null) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, error);

                return REGISTER_URL;
            }

            LocalDate parsedBirthDate = parseDate(birthDate);

            // Check duplicate email
            if (!checkEmailUnique(email)) {

                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "An account with that email already exists.");

                return REGISTER_URL;
            }

            // Create client
            insertUser(
                    firstName,
                    lastName,
                    email,
                    password,
                    parsedBirthDate,
                    "CLIENT"
            );

            securityLogger.clientAccountCreated(email);

            redirectAttributes.addFlashAttribute("successMessage", "Account created. You can now log in.");

            return "redirect:/login";

        } catch (DateTimeParseException e) {

            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, BIRTHDAY_PROMPT);

            return REGISTER_URL;

        } catch (DataAccessException e) {

            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Unable to create the account. Please try again.");

            return REGISTER_URL;

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "An unexpected error occurred. Please try again.");

            return REGISTER_URL;
        }
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

        try {
            firstName = firstName == null ? "" : firstName.trim();
            lastName = lastName == null ? "" : lastName.trim();
            email = email == null ? "" : email.trim().toLowerCase();

            String error = validateForm(
                    firstName,
                    lastName,
                    email,
                    password,
                    birthDate
            );

            if (error != null) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, error);
            }

            LocalDate parsedBirthDate = parseDate(birthDate);

            if (checkEmailUnique(email)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                        "An account with that email already exists.");
            }

            insertUser(firstName, lastName, email, password, parsedBirthDate, "ADMINISTRATOR");

            securityLogger.adminAccountCreated(authentication.getName(), email);

            redirectAttributes.addFlashAttribute("successMessage", "Administrator account created for " + email + ".");


        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, BIRTHDAY_PROMPT);


        }catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Unable to create the account. Please try again.");


        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "An unexpected error occurred. Please try again.");

        }
        return ADMIN_REGISTER_URL;
    }

    @Override
    public boolean registerAccount(String email, String password, LocalDate birthDate) {

        if (!checkEmailUnique(email)) {
            return false;
        }

        insertUser(
                "",
                "",
                email,
                password,
                birthDate,
                "CLIENT"
        );

        return true;
    }

    @Override
    public boolean checkEmailUnique(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Users WHERE userEmail = ?", Integer.class, email);
        return count != null && count == 0;
    }

    @Override
    public void createClient() {
        throw new UnsupportedOperationException("Use insertUser() to create a client account");
    }

    @Override
    public void createAdmin() {
        throw new UnsupportedOperationException("Use insertUser() to create an administrator account");
    }

    // Helper Methods

    private void insertUser(String firstName, String lastName, String email, String password, LocalDate birthDate, String role) {
        String sql = """
            INSERT INTO Users (userEmail, passwordHash, lastName, firstName, role, displayName, birthDate)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String displayName = (firstName + " " + lastName).trim();

        // BCrypt = password hash
        String passwordHash = passwordEncoder.encode(password);

        jdbcTemplate.update(sql,
                email,
                passwordHash,
                lastName,
                firstName,
                role,
                displayName,
                birthDate == null ? null : birthDate.toString());
    }

    private String validateForm(String firstName,
                                String lastName,
                                String email,
                                String password,
                                String birthDate) {
        // Names
        if (isBlank(firstName) || isBlank(lastName)) {
            return "First and last name are required.";
        }

        if (firstName.length() > 20) {
            return "First name cannot exceed 20 characters.";
        }
        if (lastName.length() > 20) {
            return "Last name cannot exceed 20 characters.";
        }
        if (!NAME_PATTERN.matcher(firstName).matches()) {
            return "First name contains invalid characters.";
        }
        if (!NAME_PATTERN.matcher(lastName).matches()) {
            return "Last name contains invalid characters.";
        }

        // Email
        if (isBlank(email)) {
            return "Email is required.";
        }
        if (email.length() > 254) {
            return "Email address is too long.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Email must end with @pennstatesoft.com.";
        }

        // Password
        if (isBlank(password)) {
            return "Password is required.";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (password.length() > 128) {
            return "Password cannot exceed 128 characters.";
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one number and one special character.";
        }

        // Birthday
        if (isBlank(birthDate)) {
            return "Birth date is required.";
        }
        try {
            LocalDate date = LocalDate.parse(birthDate);
            if (date.isAfter(LocalDate.now(ZoneId.systemDefault()))) {
                return "Birth date cannot be in the future.";
            }
        } catch (DateTimeParseException e) {
            return BIRTHDAY_PROMPT;
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value.trim());

    }
}
