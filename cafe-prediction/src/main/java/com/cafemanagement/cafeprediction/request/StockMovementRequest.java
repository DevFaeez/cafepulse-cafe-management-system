package com.cafemanagement.cafeprediction.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record StockMovementRequest(@NotNull Type type,
                                   @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal quantity,
                                   @NotBlank @Size(max = 200) String reason,
                                   @PositiveOrZero Long revision) {
    public enum Type { RECEIPT, USAGE, ADJUSTMENT, WASTE }
}
