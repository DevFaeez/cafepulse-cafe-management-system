package com.cafemanagement.cafeprediction.exception;

import org.springframework.http.HttpStatus;

public class ProductCategoryNotFoundException extends AppException{
    public ProductCategoryNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
