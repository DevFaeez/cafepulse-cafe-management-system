package com.cafemanagement.cafeprediction.response;

import lombok.Data;

@Data
public class TokenResponse {

    private String status;
    private String currentAccessToken;

}
