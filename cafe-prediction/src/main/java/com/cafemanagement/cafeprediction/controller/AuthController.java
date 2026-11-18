package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.model.enumoption.STAFF_ROLES;
import com.cafemanagement.cafeprediction.repository.AuthRepository;
import com.cafemanagement.cafeprediction.request.AuthRequest;
import com.cafemanagement.cafeprediction.request.RefreshSessionRequest;
import com.cafemanagement.cafeprediction.request.RegisterAccountRequest;
import com.cafemanagement.cafeprediction.response.AuthResponse;
import com.cafemanagement.cafeprediction.response.TokenResponse;
import com.cafemanagement.cafeprediction.service.LoginSessionService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthRepository accounts;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authentication;
    private final LoginSessionService sessions;

    public AuthController(AuthRepository accounts, PasswordEncoder passwords,
                          AuthenticationManager authentication, LoginSessionService sessions) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.authentication = authentication;
        this.sessions = sessions;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> registerStaff(@Valid @RequestBody RegisterAccountRequest request) {
        return register(request, STAFF_ROLES.ROLE_STAFF);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/kiosk")
    public ResponseEntity<Map<String, Object>> registerKiosk(@Valid @RequestBody RegisterAccountRequest request) {
        return register(request, STAFF_ROLES.ROLE_CUSTOMER);
    }

    @Transactional
    @PostMapping("/signin")
    public AuthResponse signin(@Valid @RequestBody AuthRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        accounts.lockByEmail(email);
        Authentication auth = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, request.getPassword()));
        return sessions.start(auth);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshSessionRequest request) {
        return sessions.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshSessionRequest request) {
        sessions.end(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    public Map<String, String> session(Authentication auth) {
        return Map.of("role", auth.getAuthorities().iterator().next().getAuthority());
    }

    private ResponseEntity<Map<String, Object>> register(RegisterAccountRequest request, STAFF_ROLES role) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is too long");
        }
        Staff account = new Staff();
        account.setUsername(request.username().strip());
        account.setFullname(request.fullname().strip());
        account.setEmail(request.email().strip().toLowerCase(Locale.ROOT));
        account.setPassword(passwords.encode(request.password()));
        account.setPhoneNumber(request.phoneNumber());
        account.setRole(role);
        try {
            accounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already registered");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "register_success", "role", role));
    }
}
