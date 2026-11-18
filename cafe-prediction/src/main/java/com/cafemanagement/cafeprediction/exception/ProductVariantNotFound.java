package com.cafemanagement.cafeprediction.exception;

import org.springframework.http.HttpStatus;

public class ProductVariantNotFound extends AppException {
    public ProductVariantNotFound(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

}
