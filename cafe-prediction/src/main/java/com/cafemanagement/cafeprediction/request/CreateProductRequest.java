package com.cafemanagement.cafeprediction.request;

import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.SALES_UNIT;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Enter a product name.")
    @Size(max = 100)
    private String name;

    @Column(nullable = false, length = 500)
    @NotBlank(message = "Enter a product description.")
    @Size(max = 500)
    private String description;

    private Long productCategory;

    private SALES_UNIT salesUnit;

    @Column(nullable = false, length = 300)
    @NotBlank(message = "Enter a product image URL.")
    @Size(max = 300)
    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2) //precision total number and scale bilangan perpuluhan
    @NotNull(message = "Enter a price.")
    @DecimalMin(value = "0.01", message = "The price must be greater than zero.")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PRODUCTS_STATUS status = PRODUCTS_STATUS.PRODUCT_AVAILABLE;

}
