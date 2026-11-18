package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Customer;
import java.util.Optional;
import com.cafemanagement.cafeprediction.request.CreateCustomerRequest;

public interface CustomerService {

    public Customer findCustomerById(Long id) throws Exception;

    public Customer findCustomerByPhoneNumber(String phoneNumber) throws Exception;

    Optional<Customer> findByNormalizedPhone(String phone);

    public Customer createNewCustomer(CreateCustomerRequest req) throws Exception;
}
