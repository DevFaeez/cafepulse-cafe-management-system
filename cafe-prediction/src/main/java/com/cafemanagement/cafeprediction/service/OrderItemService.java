package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.response.OrderItemResponse;

import java.util.List;

public interface OrderItemService {

    // get order item by id
    public List<OrderItem> getAllOrderItem() throws Exception;

    public List<OrderItemResponse> getAllOrderItemByOrderId(Long id) throws Exception;

}
