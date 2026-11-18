package com.cafemanagement.cafeprediction.exception;

import org.springframework.http.HttpStatus;

public class StockNotFoundException extends AppException {
    public StockNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
