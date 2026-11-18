package com.cafemanagement.cafeprediction.service;

import java.time.LocalDate;

public interface DailyOrderSequenceService {
    void lockCheckout();
    String generateOrderNumber(LocalDate date);
}
