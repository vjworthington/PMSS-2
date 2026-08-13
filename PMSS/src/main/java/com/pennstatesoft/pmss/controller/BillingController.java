package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Client payment information ("card on file") and the admin billing view.
 */
@Controller
public class BillingController {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final SecurityLogger securityLogger;
    private static final String ERROR_MESSAGE = "errorMessage";

    public BillingController(JdbcTemplate jdbcTemplate, UserService userService, SecurityLogger securityLogger) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.securityLogger = securityLogger;
    }

    // Client: manage own card on file
    @GetMapping("/billing")
    public String viewBilling(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("card", findCard(user.getUserID()));
        return "billing";
    }

    @PostMapping("/billing")
    public String saveBilling(@RequestParam(name = "cardholderName", required = false) String cardholderName,
                              @RequestParam(name = "cardType", required = false) String cardType,
                              @RequestParam(name = "cardNumber", required = false) String cardNumber,
                              @RequestParam(name = "cardExpiry", required = false) String cardExpiry,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());

        String error = validateCard(cardholderName, cardNumber, cardExpiry);
        if (error != null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, error);
            return "redirect:/billing";
        }

        saveCard(user.getUserID(), cardholderName, cardType, cardNumber, cardExpiry);
        securityLogger.billingUpdated(authentication.getName(), user.getUserID());
        redirectAttributes.addFlashAttribute("successMessage", "Payment information updated.");
        return "redirect:/billing";
    }

    // Admin: view/update any client's billing information 

    @GetMapping("/admin/billing")
    public String adminBilling(Model model, Authentication authentication) {
        model.addAttribute("user", userService.findByEmail(authentication.getName()));
        model.addAttribute("clients", findClientCards());
        return "admin/billing";
    }

    @GetMapping("/admin/billing/{userID}/edit")
    public String adminEditBilling(@PathVariable("userID") int userID,
                                   Model model,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        Map<String, Object> target = findClient(userID);
        if (target == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Client not found.");
            return "redirect:/admin/billing";
        }
        model.addAttribute("user", userService.findByEmail(authentication.getName()));
        model.addAttribute("target", target);
        model.addAttribute("card", findCard(userID));
        return "admin/billing-edit";
    }

    @PostMapping("/admin/billing/{userID}/edit")
    public String adminUpdateBilling(@PathVariable("userID") int userID,
                                     @RequestParam(name = "cardholderName", required = false) String cardholderName,
                                     @RequestParam(name = "cardType", required = false) String cardType,
                                     @RequestParam(name = "cardNumber", required = false) String cardNumber,
                                     @RequestParam(name = "cardExpiry", required = false) String cardExpiry,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        String error = validateCard(cardholderName, cardNumber, cardExpiry);
        if (error != null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, error);
            return "redirect:/admin/billing/" + userID + "/edit";
        }

        saveCard(userID, cardholderName, cardType, cardNumber, cardExpiry);
        securityLogger.billingUpdated(authentication.getName(), userID);
        redirectAttributes.addFlashAttribute("successMessage", "Billing information updated.");
        return "redirect:/admin/billing";
    }

    // helpers

    private Map<String, Object> findCard(int userID) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT userID, cardholderName, cardType, cardLast4, cardExpiry FROM Billing WHERE userID = ?",
                userID);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> findClientCards() {
        return jdbcTemplate.queryForList("""
                SELECT u.userID, u.firstName, u.lastName, u.userEmail,
                       b.cardType, b.cardLast4, b.cardExpiry
                FROM Users u
                LEFT JOIN Billing b ON u.userID = b.userID
                WHERE u.role = 'CLIENT'
                ORDER BY u.lastName, u.firstName
                """);
    }

    private Map<String, Object> findClient(int userID) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT userID, firstName, lastName, userEmail FROM Users WHERE userID = ? AND role = 'CLIENT'",
                userID);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // Persists only a masked card: name, type, last 4 digits, expiry. The full
    // number is never stored, and CVV is never collected.
    private void saveCard(int userID, String cardholderName, String cardType,
                          String cardNumber, String cardExpiry) {
        String digits = cardNumber.replaceAll("\\D", "");
        String last4 = digits.substring(digits.length() - 4);
        String type = (cardType == null || cardType.isBlank()) ? "Card" : cardType;

        jdbcTemplate.update("""
                INSERT INTO Billing (userID, cardholderName, cardType, cardLast4, cardExpiry)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(userID) DO UPDATE SET
                    cardholderName = excluded.cardholderName,
                    cardType       = excluded.cardType,
                    cardLast4      = excluded.cardLast4,
                    cardExpiry     = excluded.cardExpiry
                """, userID, cardholderName.trim(), type, last4, cardExpiry.trim());
    }

    private String validateCard(String cardholderName, String cardNumber, String cardExpiry) {
        if (cardholderName == null || cardholderName.isBlank()) {
            return "Cardholder name is required.";
        }
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        if (digits.length() < 12 || digits.length() > 19) {
            return "Enter a valid card number.";
        }
        if (cardExpiry == null || !cardExpiry.matches("\\d{2}/\\d{2}")) {
            return "Enter the expiration date as MM/YY.";
        }
        return null;
    }
}
