package com.cafemanagement.cafeprediction.exception;

import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends AppException{
    public CustomerNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
