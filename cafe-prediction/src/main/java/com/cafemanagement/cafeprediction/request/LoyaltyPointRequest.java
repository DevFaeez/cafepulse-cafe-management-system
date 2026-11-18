package com.cafemanagement.cafeprediction.request;

import lombok.Data;

@Data
public class LoyaltyPointRequest {
    private String phoneNumber;
    private int point = 0;
}
