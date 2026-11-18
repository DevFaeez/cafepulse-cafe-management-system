package com.cafemanagement.cafeprediction.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {
    @NotBlank(message = "Enter a phone number.")
    @Size(max = 32, message = "Invalid phone number.")
    private String customerPhone;

    @NotEmpty(message = "Your cart is empty.")
    @Size(max = 100, message = "Your cart can contain up to 100 line items.")
    private List<@NotNull @Valid OrderItemRequest> item;

    @Size(max = 24)
    private String promoCode;

    @PositiveOrZero
    private int points;

    @PositiveOrZero @Digits(integer = 8, fraction = 2)
    private BigDecimal expectedTotal;
}
