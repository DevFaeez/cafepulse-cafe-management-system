package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Stock;
import com.cafemanagement.cafeprediction.model.enumoption.STOCK_STATUS;
import com.cafemanagement.cafeprediction.request.CreateNewStockRequest;
import com.cafemanagement.cafeprediction.request.StockMovementRequest;
import com.cafemanagement.cafeprediction.response.StockMovementView;

import java.util.List;
import java.util.UUID;

public interface StockService {
    Stock createNewStock(CreateNewStockRequest request, String actor);
    Stock settings(Long id, CreateNewStockRequest request, String actor);
    Stock updateStockStatus(Long id, STOCK_STATUS status);
    List<Stock> getAllStock();
    List<Stock> alerts();
    StockMovementView move(Long id, UUID key, StockMovementRequest request, String actor);
    StockMovementView.History history(Long id, int page);
}
