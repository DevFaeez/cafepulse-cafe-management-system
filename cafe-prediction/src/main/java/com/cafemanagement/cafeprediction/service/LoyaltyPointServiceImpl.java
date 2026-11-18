package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.exception.CustomerLoyaltyNotFound;
import com.cafemanagement.cafeprediction.exception.ProductVariantNotFound;
import com.cafemanagement.cafeprediction.model.Customer;
import com.cafemanagement.cafeprediction.model.LoyaltyPoint;
import com.cafemanagement.cafeprediction.repository.LoyaltyPointRepository;
import com.cafemanagement.cafeprediction.request.LoyaltyPointRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoyaltyPointServiceImpl implements LoyaltyPointService {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LoyaltyPointRepository loyaltyPointRepository;
    @Override
    public LoyaltyPoint createNewLoyaltyPoint(LoyaltyPointRequest request) throws Exception {
        Customer customer = customerService.findCustomerByPhoneNumber(request.getPhoneNumber());

        LoyaltyPoint loyaltyPoint = new LoyaltyPoint();
        loyaltyPoint.setPointsBalance(0);
        loyaltyPoint.setCustomer(customer);
        LoyaltyPoint point = loyaltyPointRepository.save(loyaltyPoint);

        return point;

    }

    @Override
    public LoyaltyPoint addLoyaltyPoint(LoyaltyPointRequest request) throws Exception {
        Customer customer = customerService.findCustomerByPhoneNumber(request.getPhoneNumber());
        Optional<LoyaltyPoint> point = loyaltyPointRepository.findById(customer.getId());
        if (point.isEmpty()) {
            throw new ProductVariantNotFound("loyalty point id not found...");
        }

        point.get().setPointsBalance(point.get().getPointsBalance() + request.getPoint());
        LoyaltyPoint updated = loyaltyPointRepository.save(point.get());

        return updated;
    }

    @Override
    public LoyaltyPoint useLoyaltyPoint() throws Exception {
        return null;
    }

    @Override
    public LoyaltyPoint getCustomerLoyaltyPoint(Long id) throws Exception {
        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomer_Id(id)
                .orElseThrow(() -> new CustomerLoyaltyNotFound("customer loyalty point not found..."));
        return loyaltyPoint;
    }


    @Transactional
    public void handleCustomerLoyalty(LoyaltyPointRequest request) throws Exception {
        Customer customer = customerService.findCustomerByPhoneNumber(request.getPhoneNumber());

        LoyaltyPoint loyaltyPoint = loyaltyPointRepository
                .findByCustomer_Id(customer.getId())
                .orElseGet(() -> {
                    LoyaltyPoint lp = new LoyaltyPoint();
                    lp.setCustomer(customer);
                    lp.setPointsBalance(0);
                    return loyaltyPointRepository.save(lp);
                });

        loyaltyPoint.setPointsBalance(loyaltyPoint.getPointsBalance() + request.getPoint());
        loyaltyPointRepository.save(loyaltyPoint);
    }
}
