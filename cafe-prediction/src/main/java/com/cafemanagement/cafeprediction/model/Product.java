package com.cafemanagement.cafeprediction.model;

import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.SALES_UNIT;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Enter a product name.")
    private String name;

    @Column(nullable = false, length = 500)
    @NotBlank(message = "Enter a product description.")
    private String description;

    @ManyToOne
    @JoinColumn(name = "product_category_id", nullable = false)
    private ProductCategory productCategory;

    @Column(nullable = false, length = 300)
    @NotBlank(message = "Enter a product image URL.")
    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2) //precision total number and scale bilangan perpuluhan
    @NotNull(message = "Enter a price.")
    @DecimalMin(value = "0.01", message = "The price must be greater than zero.")
    private BigDecimal price;

    @JsonIgnore
    @OneToMany(mappedBy = "product", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<ProductVariant> productVariants = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PRODUCTS_STATUS status = PRODUCTS_STATUS.PRODUCT_AVAILABLE;

    @Enumerated(EnumType.STRING)
    private SALES_UNIT salesUnit;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
