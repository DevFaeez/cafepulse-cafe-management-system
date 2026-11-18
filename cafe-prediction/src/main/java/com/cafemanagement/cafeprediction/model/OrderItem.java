package com.cafemanagement.cafeprediction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.cafemanagement.cafeprediction.model.enumoption.SALES_UNIT;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // order_item_id

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @BatchSize(size = 32)
    @OneToMany(mappedBy = "orderItemId", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<OrderItemVariant> orderItemVariants = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price; // snapshot masa order dibuat

    @Column(length = 200)
    private String note;

    @Column(length = 100)
    private String productNameSnapshot;

    @Enumerated(EnumType.STRING)
    private SALES_UNIT salesUnitSnapshot;

    public BigDecimal lineTotal() {
        BigDecimal unit = price;
        for (OrderItemVariant variant : orderItemVariants) {
            if (variant.getExtraPrice() != null) unit = unit.add(variant.getExtraPrice());
        }
        return unit.multiply(BigDecimal.valueOf(quantity));
    }
}
