package com.cafemanagement.cafeprediction.request;

import com.cafemanagement.cafeprediction.model.enumoption.USER_STATUS;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    private String phoneNumber;

}
