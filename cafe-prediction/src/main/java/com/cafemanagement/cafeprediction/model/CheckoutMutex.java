package com.cafemanagement.cafeprediction.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CheckoutMutex {
    @Id
    private Integer id;
}
