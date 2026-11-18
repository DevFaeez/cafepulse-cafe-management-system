package com.cafemanagement.cafeprediction.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(max = 200) String fullname,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @Size(max = 12) @Pattern(regexp = "^[0-9+\\-\\s()]*$") String phoneNumber) {
}
