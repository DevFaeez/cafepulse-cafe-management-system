package com.cafemanagement.cafeprediction.response;

import com.cafemanagement.cafeprediction.model.enumoption.STAFF_ROLES;
import lombok.Data;

import java.time.Instant;

@Data
public class AuthResponse {

    private String accessToken;
    private String message;
    private String status;
    private String refreshToken;
    private STAFF_ROLES role;
    private Instant expiresAt;



}
