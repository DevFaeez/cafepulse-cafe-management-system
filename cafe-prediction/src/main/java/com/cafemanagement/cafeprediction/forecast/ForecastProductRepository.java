package com.cafemanagement.cafeprediction.forecast;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastProductRepository extends JpaRepository<ForecastProduct, Drink> {}
