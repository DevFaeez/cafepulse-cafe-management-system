package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Staff;

import java.util.Optional;

public interface AuthService {
    public Optional<Staff> findUserByJwtToken(String jwt) throws Exception;
    public Optional<Staff> findUserByEmail(String email) throws Exception;
}
