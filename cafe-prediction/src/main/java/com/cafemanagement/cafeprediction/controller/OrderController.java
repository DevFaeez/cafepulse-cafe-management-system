package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.Customer;
import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.request.OrderItemRequest;
import com.cafemanagement.cafeprediction.request.OrderRequest;
import com.cafemanagement.cafeprediction.response.CheckoutReceipt;
import com.cafemanagement.cafeprediction.response.CheckoutQuote;
import com.cafemanagement.cafeprediction.service.Promotions;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import com.cafemanagement.cafeprediction.service.CustomerService;
import com.cafemanagement.cafeprediction.service.OrderService;
import org.hibernate.query.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private Promotions promotions;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/create")
    public ResponseEntity<CheckoutReceipt> createNewOrder(@RequestHeader("Idempotency-Key") UUID requestId,
                                                         @Valid @RequestBody OrderRequest request, Principal kiosk) throws Exception {
        Orders order = orderService.createNewOrder(kiosk.getName(), requestId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CheckoutReceipt.of(order));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/quote")
    public CheckoutQuote quote(@Valid @RequestBody OrderRequest request) {
        return orderService.quote(request);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/offers")
    public List<Promotions.Offer> offers() {
        return promotions.active();
    }

    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @GetMapping("/allorder")
    public ResponseEntity<List<Orders>> getAllOrder() throws Exception {
        List<Orders> allOrder = orderService.getAllOrder();
        return new ResponseEntity<>(allOrder, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    @GetMapping("/pendingpreparing/order")
    public ResponseEntity<List<Orders>> getAllOrderPendingPreparing() throws Exception {
        List<Orders> allOrderPendingPreparing = orderService.getAllOrderPendingPreparing();
        return new ResponseEntity<>(allOrderPendingPreparing, HttpStatus.OK);
    }

}
