package com.cafemanagement.cafeprediction.config;

import com.cafemanagement.cafeprediction.service.LoginSessionService;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AppConfig {

    private final LoginSessionService sessions;
    private final String allowedOrigin;

    public AppConfig(LoginSessionService sessions, @Value("${cafe.cors.origin}") String allowedOrigin) {
        this.sessions = sessions;
        this.allowedOrigin = allowedOrigin;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(mgmt -> mgmt.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/signin", "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/kiosk").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/auth/session").authenticated()
                        .requestMatchers("/auth/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/product/available", "/api/admin/productcategory",
                                "/api/admin/productvariant/{id}", "/api/admin/productvariant/get/{id}")
                                .hasAnyRole("CUSTOMER", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/order/create", "/api/order/quote").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/order/offers").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/order/{id}/payment/void", "/api/order/{id}/close").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/stock/create").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/stock/{id}/settings").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/stock/alerts").hasRole("MANAGER")
                        .requestMatchers("/api/order/**", "/api/stock/**").hasAnyRole("STAFF", "MANAGER")
                        .requestMatchers("/api/product/**", "/api/admin/**", "/api/customer/**").hasRole("MANAGER")
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":\"unauthenticated\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":\"forbidden\"}");
                        }))
                .addFilterBefore(new JwtTokenValidator(sessions), BasicAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(List.of(allowedOrigin));
            cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
            cfg.setExposedHeaders(List.of("Authorization"));
            cfg.setAllowCredentials(true);
            cfg.setMaxAge(3600L);
            return cfg;
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
