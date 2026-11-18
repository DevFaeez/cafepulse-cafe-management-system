package com.cafemanagement.cafeprediction.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderItemRequest {
    @NotNull
    @Positive
    private Long product;

    @Min(value = 1, message = "The minimum quantity is 1.")
    @Max(value = 99, message = "The maximum quantity is 99 per line item.")
    private int quantity;

    @Size(max = 20)
    private List<@NotNull @Positive Long> variantIds;

    @Size(max = 200)
    private String note;
}
