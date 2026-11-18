package com.cafemanagement.cafeprediction.response;

import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.model.ProductVariant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderItemMapper {

    public static OrderItemResponse toDTO(OrderItem item) {

        // Convert each OrderItemVariant into category/value pair
        List<Map<String, String>> variantList = item.getOrderItemVariants() != null
                ? item.getOrderItemVariants().stream()
                .map(oiv -> {
                    ProductVariant pv = oiv.getProductVaraintId();
                    return Map.of(
                            "category", oiv.getCategorySnapshot() == null ? pv.getCategory().getName() : oiv.getCategorySnapshot(),
                            "value", oiv.getValueSnapshot() == null ? pv.getValue() : oiv.getValueSnapshot()
                    );
                })
                .collect(Collectors.toList())
                : List.of();

        return OrderItemResponse.builder()
                .orderNumber(item.getOrder().getOrderSequence())
                .productName(item.getProductNameSnapshot() == null ? item.getProduct().getName() : item.getProductNameSnapshot())
                .variants(variantList)
                .quantity(item.getQuantity())
                .note(item.getNote())
                .build();
    }
}
