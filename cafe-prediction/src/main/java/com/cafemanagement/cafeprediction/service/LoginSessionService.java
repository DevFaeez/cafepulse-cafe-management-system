package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.config.JwtProvider;
import com.cafemanagement.cafeprediction.model.LoginSession;
import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.model.enumoption.USER_STATUS;
import com.cafemanagement.cafeprediction.repository.AuthRepository;
import com.cafemanagement.cafeprediction.repository.LoginSessionRepository;
import com.cafemanagement.cafeprediction.response.AuthResponse;
import com.cafemanagement.cafeprediction.response.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LoginSessionService {
    private final JwtProvider tokens;
    private final LoginSessionRepository sessions;
    private final AuthRepository accounts;
    private final long refreshMillis;

    public LoginSessionService(JwtProvider tokens, LoginSessionRepository sessions, AuthRepository accounts,
                               @Value("${jwt.refreshExpiration}") long refreshMillis) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.accounts = accounts;
        this.refreshMillis = refreshMillis;
    }

    @Transactional
    public AuthResponse start(Authentication auth) {
        Staff account = accounts.findByEmail(auth.getName()).orElseThrow(LoginSessionService::invalidSession);
        if (account.getStatus() != USER_STATUS.USER_ACTIVE) {
            throw invalidSession();
        }
        Instant now = Instant.now();
        sessions.deleteByAccountIdAndExpiresAtBefore(account.getId(), now);
        LoginSession session = sessions.save(new LoginSession(UUID.randomUUID().toString(), account, now.plusMillis(refreshMillis)));
        AuthResponse response = new AuthResponse();
        response.setAccessToken(tokens.accessToken(authentication(account), session.getId(), session.getExpiresAt()));
        response.setRefreshToken(tokens.refreshToken(account.getEmail(), session.getId(), session.getExpiresAt()));
        response.setExpiresAt(session.getExpiresAt());
        response.setRole(account.getRole());
        response.setMessage("Login Success");
        response.setStatus("login_success");
        return response;
    }

    @Transactional(readOnly = true)
    public Authentication authenticate(String accessToken) {
        return authentication(activeSession(accessToken, JwtProvider.ACCESS).getAccount());
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        LoginSession session = activeSession(refreshToken, JwtProvider.REFRESH);
        TokenResponse response = new TokenResponse();
        response.setCurrentAccessToken(tokens.accessToken(authentication(session.getAccount()), session.getId(), session.getExpiresAt()));
        response.setStatus("token_refreshed");
        return response;
    }

    @Transactional
    public void end(String refreshToken) {
        Claims claims;
        try {
            claims = tokens.readToken(refreshToken, JwtProvider.REFRESH);
        } catch (JwtException | IllegalArgumentException ex) {
            return;
        }
        sessions.deleteById(claims.getId());
    }

    private LoginSession activeSession(String token, String type) {
        Claims claims;
        try {
            claims = tokens.readToken(token, type);
        } catch (JwtException | IllegalArgumentException ex) {
            throw invalidSession();
        }
        return sessions.findByIdAndExpiresAtAfter(claims.getId(), Instant.now())
                .filter(session -> claims.getSubject().equals(session.getAccount().getEmail()))
                .filter(session -> session.getAccount().getStatus() == USER_STATUS.USER_ACTIVE)
                .orElseThrow(LoginSessionService::invalidSession);
    }

    private static Authentication authentication(Staff account) {
        return UsernamePasswordAuthenticationToken.authenticated(account.getEmail(), null,
                List.of(new SimpleGrantedAuthority(account.getRole().name())));
    }

    private static BadCredentialsException invalidSession() {
        return new BadCredentialsException("Invalid or expired session");
    }
}
