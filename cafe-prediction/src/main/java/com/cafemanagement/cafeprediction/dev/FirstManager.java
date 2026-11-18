package com.cafemanagement.cafeprediction.dev;

import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.model.enumoption.STAFF_ROLES;
import com.cafemanagement.cafeprediction.repository.AuthRepository;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@Profile("bootstrap-manager")
class FirstManager implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FirstManager.class);
    private final AuthRepository accounts;
    private final PasswordEncoder passwords;
    private final Environment environment;
    private final Validator validator;

    FirstManager(AuthRepository accounts, PasswordEncoder passwords, Environment environment, Validator validator) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.environment = environment;
        this.validator = validator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.existsByRole(STAFF_ROLES.ROLE_MANAGER)) {
            log.info("A manager account already exists");
            return;
        }

        String email = environment.getRequiredProperty("CAFE_BOOTSTRAP_MANAGER_EMAIL").strip().toLowerCase(Locale.ROOT);
        String password = environment.getRequiredProperty("CAFE_BOOTSTRAP_MANAGER_PASSWORD");
        if (password.isBlank() || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Manager password must have 12 or more characters and at most 72 UTF-8 bytes");
        }

        Staff manager = new Staff();
        manager.setUsername("manager-bootstrap");
        manager.setFullname("CafePulse Manager");
        manager.setEmail(email);
        manager.setPassword(passwords.encode(password));
        manager.setRole(STAFF_ROLES.ROLE_MANAGER);
        if (!validator.validate(manager).isEmpty()) {
            throw new IllegalArgumentException("Invalid manager account configuration");
        }
        accounts.saveAndFlush(manager);
        log.info("First manager account created");
    }
}
