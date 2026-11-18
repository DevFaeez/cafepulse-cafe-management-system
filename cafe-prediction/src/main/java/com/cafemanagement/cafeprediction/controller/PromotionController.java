package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.Promo;
import com.cafemanagement.cafeprediction.service.Promotions;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/promotions")
@PreAuthorize("hasRole('MANAGER')")
public class PromotionController {
    private final Promotions promotions;

    public PromotionController(Promotions promotions) {
        this.promotions = promotions;
    }

    @GetMapping
    public List<Promo> all() {
        return promotions.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Promo create(@Valid @RequestBody Promotions.Input input) {
        return promotions.save(null, input);
    }

    @PutMapping("/{id}")
    public Promo edit(@PathVariable Long id, @Valid @RequestBody Promotions.Input input) {
        return promotions.save(id, input);
    }
}
