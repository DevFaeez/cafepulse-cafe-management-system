package com.cafemanagement.cafeprediction.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtProvider {
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    private final SecretKey key;
    private final long accessMillis;

    public JwtProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long accessMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMillis = accessMillis;
    }

    public String accessToken(Authentication auth, String sessionId, Instant sessionExpiry) {
        Instant accessExpiry = Instant.now().plusMillis(accessMillis);
        Instant expiresAt = accessExpiry.isBefore(sessionExpiry) ? accessExpiry : sessionExpiry;
        String roles = auth.getAuthorities().stream().map(Object::toString).collect(Collectors.joining(","));
        return Jwts.builder()
                .setSubject(auth.getName())
                .setId(sessionId)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expiresAt))
                .claim("type", ACCESS)
                .claim("roles", roles)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String refreshToken(String email, String sessionId, Instant expiresAt) {
        return Jwts.builder()
                .setSubject(email)
                .setId(sessionId)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expiresAt))
                .claim("type", REFRESH)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims readToken(String token, String type) {
        return Jwts.parserBuilder().setSigningKey(key).require("type", type)
                .build().parseClaimsJws(token).getBody();
    }

}
