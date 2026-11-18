package com.cafemanagement.cafeprediction.exception;

import org.springframework.http.HttpStatus;

public class ProductVariantCategoryNotFoundException extends AppException{
    public ProductVariantCategoryNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
