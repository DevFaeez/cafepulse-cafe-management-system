package com.cafemanagement.cafeprediction.request;

import com.cafemanagement.cafeprediction.model.enumoption.STOCK_STATUS;
import lombok.Data;

@Data
public class UpdateStockRequest {

    private STOCK_STATUS status;
}
