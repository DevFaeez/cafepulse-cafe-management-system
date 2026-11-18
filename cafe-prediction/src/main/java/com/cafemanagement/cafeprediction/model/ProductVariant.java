package com.cafemanagement.cafeprediction.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private boolean defaultVariant = false;

    @ManyToOne
    @JoinColumn(name = "variant_category_id", nullable = false)
    private ProductVariantCategory category;

    @Column(nullable = false, length = 120)
    @NotBlank(message = "product varirant value cannot be blanl")
    private String value;

    @Column(nullable = false)
    @NotNull(message = "extra price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "extra price must be 0 or more")
    private BigDecimal extraPrice;


}
