package com.cafemanagement.cafeprediction.request;

import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

public class UpdateProductRequest {

        @Column(nullable = false, length = 100)
        @NotBlank(message = "Enter a product name.")
        private String name;

        @Column(nullable = false, length = 500)
        @NotBlank(message = "Enter a product description.")
        private String description;

        @Column(nullable = false, length = 300)
        @NotBlank(message = "Enter a product image URL.")
        private String productImage;

        @Column(nullable = false, precision = 10, scale = 2) //precision total number and scale bilangan perpuluhan
        @NotNull(message = "Enter a price.")
        @DecimalMin(value = "0.01", message = "The price must be greater than zero.")
        private BigDecimal price;

}
