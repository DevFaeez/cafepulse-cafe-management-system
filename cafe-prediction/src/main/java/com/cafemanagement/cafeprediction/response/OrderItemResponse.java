package com.cafemanagement.cafeprediction.response;

import com.cafemanagement.cafeprediction.model.OrderItemVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private String orderNumber;  // e.g. "7"
    private String productName;  // e.g. "Milo"
    private List<Map<String, String>> variants;  // e.g. [{category:"Size", value:"Small"}]
    private int quantity;
    private String note;

}
