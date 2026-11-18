package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.request.OrderRequest;
import com.cafemanagement.cafeprediction.response.CheckoutQuote;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    Orders createNewOrder(String kiosk, UUID requestId, OrderRequest request) throws Exception;
    CheckoutQuote quote(OrderRequest request);
    List<Orders> getAllOrder() throws Exception;
    List<Orders> getAllOrderPendingPreparing() throws Exception;
}
