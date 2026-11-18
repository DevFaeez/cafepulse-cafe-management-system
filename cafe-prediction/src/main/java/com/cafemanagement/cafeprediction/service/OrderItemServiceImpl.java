package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.exception.ProductVariantNotFound;
import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.repository.OrderItemRepository;
import com.cafemanagement.cafeprediction.response.OrderItemMapper;
import com.cafemanagement.cafeprediction.response.OrderItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Override
    public List<OrderItem> getAllOrderItem() throws Exception {

        return null;
    }

    @Override
    public List<OrderItemResponse> getAllOrderItemByOrderId(Long id) throws Exception {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrder_Id(id);
        if (orderItems.isEmpty()) {
            throw new ProductVariantNotFound("Order item not found...");
        }

        return orderItems.stream()
                .map(OrderItemMapper::toDTO)
                .collect(Collectors.toList());
    }


}
