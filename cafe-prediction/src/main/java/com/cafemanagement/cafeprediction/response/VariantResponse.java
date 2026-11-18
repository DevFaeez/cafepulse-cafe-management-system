package com.cafemanagement.cafeprediction.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantResponse {
    private String variantId;
    private String value;
    private double extraPrice;


}
