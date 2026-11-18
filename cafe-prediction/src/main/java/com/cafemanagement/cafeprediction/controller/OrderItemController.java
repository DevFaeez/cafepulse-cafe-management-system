package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.response.OrderItemResponse;
import com.cafemanagement.cafeprediction.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
public class OrderItemController {

    @Autowired
    private OrderItemService orderItemService;

    @GetMapping("/{id}/item")
    public ResponseEntity<List<OrderItemResponse>> getOrderItemByOrderId(@PathVariable Long id) throws Exception {
        List<OrderItemResponse> orderItemList = orderItemService.getAllOrderItemByOrderId(id);
        return new ResponseEntity<>(orderItemList, HttpStatus.OK);
    }

}
