package com.cafemanagement.cafeprediction.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
//    @JsonBackReference
    private OrderItem orderItemId;


    @ManyToOne
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVaraintId;

    @Column(precision = 10, scale = 2)
    private BigDecimal extraPrice;

    @Column(length = 100)
    private String categorySnapshot;

    @Column(length = 120)
    private String valueSnapshot;

}
