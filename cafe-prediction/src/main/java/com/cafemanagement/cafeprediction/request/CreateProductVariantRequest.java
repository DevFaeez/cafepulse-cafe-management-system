package com.cafemanagement.cafeprediction.request;

import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.ProductVariantCategory;
import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Data
public class CreateProductVariantRequest {

    @NotNull @Positive
    private Long product;
    @NotNull @Positive
    private Long category;
    @NotBlank @Size(max = 120)
    private String value;
    @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2)
    private BigDecimal extraPrice;

}
