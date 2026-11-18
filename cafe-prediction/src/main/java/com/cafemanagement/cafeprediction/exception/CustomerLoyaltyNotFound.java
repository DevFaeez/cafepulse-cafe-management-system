package com.cafemanagement.cafeprediction.exception;

import org.springframework.http.HttpStatus;

public class CustomerLoyaltyNotFound extends AppException {
    public CustomerLoyaltyNotFound(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
