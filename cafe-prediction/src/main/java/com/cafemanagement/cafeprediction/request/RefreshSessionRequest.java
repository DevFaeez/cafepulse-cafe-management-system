package com.cafemanagement.cafeprediction.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshSessionRequest(@NotBlank @Size(max = 4096) String refreshToken) {
}
