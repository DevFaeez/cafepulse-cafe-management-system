package com.cafemanagement.cafeprediction.config;

import com.cafemanagement.cafeprediction.service.LoginSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtTokenValidator extends OncePerRequestFilter {
    private final LoginSessionService sessions;

    public JwtTokenValidator(LoginSessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(JwtConstant.JWT_HEADER);
        if (header != null && header.startsWith(JwtConstant.TOKEN_PREFIX)) {
            try {
                SecurityContextHolder.getContext().setAuthentication(
                        sessions.authenticate(header.substring(JwtConstant.TOKEN_PREFIX.length())));
            } catch (BadCredentialsException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
