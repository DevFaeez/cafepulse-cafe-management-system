package com.cafemanagement.cafeprediction.request;

import jakarta.persistence.Column;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class ManageProductVariantCategoryRequest {
    @Column(nullable = false)
    @NotBlank @Size(max = 100)
    public String name;
}
