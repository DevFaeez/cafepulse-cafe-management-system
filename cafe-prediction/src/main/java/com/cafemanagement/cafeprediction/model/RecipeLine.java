package com.cafemanagement.cafeprediction.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner_type", "owner_id", "stock_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeLine {
    public enum Owner { PRODUCT, VARIANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private Owner ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_stock_id")
    private Stock replacementStock;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    public RecipeLine(Owner ownerType, long ownerId, Stock stock, BigDecimal quantity, Stock replacementStock) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.stock = stock;
        this.quantity = replacementStock == null ? quantity : BigDecimal.ZERO;
        this.replacementStock = replacementStock;
    }
}
