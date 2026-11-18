package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.model.enumoption.STAFF_ROLES;
import com.cafemanagement.cafeprediction.model.enumoption.USER_STATUS;
import com.cafemanagement.cafeprediction.repository.AuthRepository;
import com.cafemanagement.cafeprediction.repository.LoginSessionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerAccountsController {
    private final AuthRepository accounts;
    private final LoginSessionRepository sessions;
    private final PasswordEncoder passwords;

    public ManagerAccountsController(AuthRepository accounts, LoginSessionRepository sessions, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwords = passwords;
    }

    @GetMapping
    public List<Account> list() {
        return accounts.findByRoleNotOrderByIdAsc(STAFF_ROLES.ROLE_MANAGER).stream().map(Account::of).toList();
    }

    @PutMapping("/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void active(@PathVariable Long id, @Valid @RequestBody Activation input) {
        Staff account = manageable(id);
        account.setStatus(input.active() ? USER_STATUS.USER_ACTIVE : USER_STATUS.USER_INACTIVE);
        if (!input.active()) sessions.revokeAccount(id);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void reset(@PathVariable Long id, @Valid @RequestBody Password input) {
        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is too long.");
        }
        Staff account = manageable(id);
        account.setPassword(passwords.encode(input.password()));
        sessions.revokeAccount(id);
    }

    private Staff manageable(Long id) {
        return accounts.lockById(id).filter(account -> account.getRole() != STAFF_ROLES.ROLE_MANAGER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff or kiosk account not found."));
    }

    public record Activation(@NotNull Boolean active) {}
    public record Password(@NotBlank @Size(min = 12, max = 72) String password) {}
    public record Account(Long id, String username, String fullname, String email, STAFF_ROLES role, USER_STATUS status) {
        private static Account of(Staff account) {
            return new Account(account.getId(), account.getUsername(), account.getFullname(), account.getEmail(),
                    account.getRole(), account.getStatus());
        }
    }
}
