package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.Transaction;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        userService.saveUser(user);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("transactions", userService.getTransactionsForUser(username));
        return "home";
    }

    @GetMapping("/account")
    public String showAccountPage(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username);
        List<Transaction> scheduled = userService.getPendingScheduledTransactions(username);

        model.addAttribute("user", user);
        model.addAttribute("scheduledTransactions", scheduled);
        return "account";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam double amount, Principal principal) {
        userService.deposit(principal.getName(), amount);
        return "redirect:/account";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam double amount, Principal principal) {
        userService.withdraw(principal.getName(), amount);
        return "redirect:/account";
    }

    @PostMapping("/transfer")
    public String transfer(
            @RequestParam String targetAccountNumber,
            @RequestParam double amount,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer hour,
            @RequestParam(required = false) Integer minute,
            Principal principal
    ) {
        LocalDateTime scheduled = null;
        if (date != null && !date.isBlank()) {
            hour = (hour == null) ? 0 : hour;
            minute = (minute == null) ? 0 : minute;
            scheduled = LocalDateTime.parse(date + "T" +
                    String.format("%02d", hour) + ":" +
                    String.format("%02d", minute) + ":00");
        }
        userService.transfer(principal.getName(), targetAccountNumber, amount, scheduled);
        return "redirect:/account";
    }

    @PostMapping("/cancelTransaction")
    public String cancelTransaction(@RequestParam Long transactionId, Principal principal) {
        userService.cancelScheduledTransaction(transactionId, principal.getName());
        return "redirect:/account";
    }

    @GetMapping("/editTransaction")
    public String editTransactionForm(@RequestParam Long transactionId, Model model, Principal principal) {
        Transaction tx = userService.getScheduledTransaction(transactionId, principal.getName());
        model.addAttribute("transaction", tx);
        return "edit_transaction";
    }

    @PostMapping("/updateTransaction")
    public String updateTransaction(
            @RequestParam Long transactionId,
            @RequestParam String date,
            @RequestParam Integer hour,
            @RequestParam Integer minute,
            Principal principal
    ) {
        LocalDateTime newDate = LocalDateTime.parse(date + "T" +
                String.format("%02d", hour) + ":" +
                String.format("%02d", minute) + ":00");

        userService.updateScheduledTransaction(transactionId, principal.getName(), newDate);
        return "redirect:/account";
    }
}
