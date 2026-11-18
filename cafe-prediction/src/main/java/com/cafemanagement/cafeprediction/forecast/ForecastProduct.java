package com.cafemanagement.cafeprediction.forecast;

import com.cafemanagement.cafeprediction.model.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ForecastProduct {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Drink drink;

    @OneToOne(optional = false)
    @JoinColumn(nullable = false, unique = true)
    private Product product;

    public ForecastProduct(Drink drink, Product product) {
        this.drink = drink;
        this.product = product;
    }
}
