package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.repository.CustomerRepository;
import com.cafemanagement.cafeprediction.service.CustomerLoyaltyDashboard;
import com.cafemanagement.cafeprediction.service.CustomerService;
import com.cafemanagement.cafeprediction.service.OrderRewards;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/loyalty")
@PreAuthorize("hasRole('MANAGER')")
public class LoyaltyHistoryController {
    private final CustomerService customers;
    private final OrderRewards rewards;
    private final CustomerRepository customerRecords;
    private final CustomerLoyaltyDashboard dashboard;

    public LoyaltyHistoryController(CustomerService customers, OrderRewards rewards, CustomerRepository customerRecords,
                                    CustomerLoyaltyDashboard dashboard) {
        this.customers = customers;
        this.rewards = rewards;
        this.customerRecords = customerRecords;
        this.dashboard = dashboard;
    }

    @GetMapping("/overview")
    public ResponseEntity<CustomerLoyaltyDashboard.Overview> overview() {
        return privateResponse(dashboard.overview());
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerLoyaltyDashboard.Directory> directory(@Valid @RequestBody Search input) {
        return privateResponse(dashboard.directory(input.phone(), input.page()));
    }

    @GetMapping("/customers/{id}/history")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderRewards.History> customerHistory(@PathVariable long id) {
        return privateResponse(rewards.history(customerRecords.findById(id).orElseThrow(LoyaltyHistoryController::notFound)));
    }

    @PostMapping("/history")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderRewards.History> history(@Valid @RequestBody Lookup input) {
        return privateResponse(rewards.history(customers.findByNormalizedPhone(input.phone())
                .orElseThrow(LoyaltyHistoryController::notFound)));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found.");
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    public record Search(@Size(max = 32) String phone, @Min(0) @Max(1000000) int page) {}
    public record Lookup(@NotBlank @Size(max = 32) String phone) {}
}
