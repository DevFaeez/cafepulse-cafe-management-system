package com.cafemanagement.cafeprediction.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class DailyOrderSequenceTests {
    @Autowired
    private DailyOrderSequenceService sequences;

    @Test
    void nextBusinessDayHasItsOwnSequence() {
        LocalDate day = LocalDate.of(2299, 7, 1);
        sequences.lockCheckout();
        assertEquals("1", sequences.generateOrderNumber(day));
        assertEquals("2", sequences.generateOrderNumber(day));
        assertEquals("1", sequences.generateOrderNumber(day.plusDays(1)));
    }
}
