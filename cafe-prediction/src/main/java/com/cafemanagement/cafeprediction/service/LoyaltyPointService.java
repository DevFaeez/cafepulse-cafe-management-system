package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.LoyaltyPoint;
import com.cafemanagement.cafeprediction.request.LoyaltyPointRequest;

public interface LoyaltyPointService {

    // create new loyalty point
    public LoyaltyPoint createNewLoyaltyPoint(LoyaltyPointRequest request) throws Exception;

    // add point
    public LoyaltyPoint addLoyaltyPoint(LoyaltyPointRequest request) throws Exception;

    // use loyalty point
    public LoyaltyPoint useLoyaltyPoint() throws Exception;

    // find loyalty point
    public LoyaltyPoint getCustomerLoyaltyPoint(Long id) throws Exception;

    public void handleCustomerLoyalty(LoyaltyPointRequest request) throws Exception;
}
