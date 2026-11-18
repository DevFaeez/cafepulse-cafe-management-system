package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.Stock;
import com.cafemanagement.cafeprediction.request.CreateNewStockRequest;
import com.cafemanagement.cafeprediction.request.StockMovementRequest;
import com.cafemanagement.cafeprediction.request.UpdateStockRequest;
import com.cafemanagement.cafeprediction.response.StockMovementView;
import com.cafemanagement.cafeprediction.service.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
public class StockController {
    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Stock create(@Valid @RequestBody CreateNewStockRequest request, Principal actor) {
        return stock.createNewStock(request, actor.getName());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}/settings")
    public Stock settings(@PathVariable Long id, @Valid @RequestBody CreateNewStockRequest request, Principal actor) {
        return stock.settings(id, request, actor.getName());
    }

    @PostMapping("/{id}/update")
    public Stock legacyStatus(@PathVariable Long id, @RequestBody UpdateStockRequest request) {
        return stock.updateStockStatus(id, request.getStatus());
    }

    @GetMapping("/all")
    public List<Stock> all() {
        return stock.getAllStock();
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/alerts")
    public List<Stock> alerts() {
        return stock.alerts();
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementView move(@PathVariable Long id, @RequestHeader("Idempotency-Key") UUID key,
                                  @Valid @RequestBody StockMovementRequest request, Principal actor) {
        return stock.move(id, key, request, actor.getName());
    }

    @GetMapping("/{id}/movements")
    public StockMovementView.History history(@PathVariable Long id, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return stock.history(id, page);
    }
}
