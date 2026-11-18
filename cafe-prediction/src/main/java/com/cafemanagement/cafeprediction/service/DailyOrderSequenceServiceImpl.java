package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.DailyOrderSequence;
import com.cafemanagement.cafeprediction.repository.DailyOrderSequenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class DailyOrderSequenceServiceImpl implements DailyOrderSequenceService {
    private final DailyOrderSequenceRepository sequences;
    private final JdbcTemplate jdbc;

    public DailyOrderSequenceServiceImpl(DailyOrderSequenceRepository sequences, JdbcTemplate jdbc) {
        this.sequences = sequences;
        this.jdbc = jdbc;
    }

    @Override
    public void lockCheckout() {
        // MySQL holds the single row lock through commit or rollback, even across application instances.
        jdbc.update("INSERT INTO checkout_mutex (id) VALUES (1) ON DUPLICATE KEY UPDATE id = 1");
    }

    @Override
    public String generateOrderNumber(LocalDate date) {
        DailyOrderSequence counter = sequences.findFirstByDateOrderBySequenceDescIdDesc(date).orElseGet(() -> {
            DailyOrderSequence fresh = new DailyOrderSequence();
            fresh.setDate(date);
            return fresh;
        });
        counter.setSequence(counter.getSequence() + 1);
        sequences.save(counter);
        return Integer.toString(counter.getSequence());
    }
}
