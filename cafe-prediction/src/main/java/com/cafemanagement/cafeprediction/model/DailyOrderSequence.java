package com.cafemanagement.cafeprediction.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class DailyOrderSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date; // e.g. 2025-10-09
    private int sequence;   // starts from 0 or 1 each day
}
