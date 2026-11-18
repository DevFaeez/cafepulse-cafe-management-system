package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.PAYMENT_METHOD;
import com.cafemanagement.cafeprediction.response.StaffOrderView;
import com.cafemanagement.cafeprediction.service.StaffOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/order")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
public class StaffOrderController {
    private final StaffOrderService service;

    public StaffOrderController(StaffOrderService service) {
        this.service = service;
    }

    @GetMapping("/workbench")
    public List<StaffOrderView> active() {
        return service.active();
    }

    @GetMapping("/history")
    public StaffOrderView.History history(@RequestParam(defaultValue = "0") @Min(0) int page) {
        return service.history(page);
    }

    @PostMapping("/{id}/payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordPayment(@PathVariable long id, @Valid @RequestBody PaymentRequest request, Principal actor) {
        service.recordPayment(id, request.revision(), request.method(), request.amount(), actor.getName());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{id}/payment/void")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void voidPayment(@PathVariable long id, @Valid @RequestBody VoidRequest request, Principal actor) {
        service.voidPayment(id, request.revision(), request.paymentId(), request.reason(), actor.getName());
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request, Principal actor) {
        service.changeStatus(id, request.revision(), request.status(), actor.getName());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable long id, @Valid @RequestBody CloseRequest request, Principal actor) {
        service.close(id, request.revision(), request.refund(), Boolean.TRUE.equals(request.restoreStock()), request.reason(), actor.getName());
    }

    public record CloseRequest(@NotNull @PositiveOrZero Long revision, @NotNull Boolean refund,
                               @NotBlank @Size(max = 200) String reason, Boolean restoreStock) {}

    public record PaymentRequest(@NotNull @PositiveOrZero Long revision, @NotNull PAYMENT_METHOD method,
                                 @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal amount) {}
    public record VoidRequest(@NotNull @PositiveOrZero Long revision, @NotNull @Positive Long paymentId,
                              @NotBlank @Size(max = 200) String reason) {}
    public record StatusRequest(@NotNull @PositiveOrZero Long revision, @NotNull ORDER_STATUS status) {}
}
