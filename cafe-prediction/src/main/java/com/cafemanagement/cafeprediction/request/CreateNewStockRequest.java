package com.cafemanagement.cafeprediction.request;

import com.cafemanagement.cafeprediction.model.enumoption.STOCK_UNIT;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateNewStockRequest(@NotBlank @Size(max = 150) String stockName,
                                    STOCK_UNIT unit,
                                    @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal minimum,
                                    @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal openingQuantity) {}
