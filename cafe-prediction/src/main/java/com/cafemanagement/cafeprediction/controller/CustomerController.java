package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.Customer;
import com.cafemanagement.cafeprediction.request.CreateCustomerRequest;
import com.cafemanagement.cafeprediction.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('MANAGER')")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/create")
    public ResponseEntity<Customer> createNewCustomer(@RequestBody CreateCustomerRequest req) throws Exception {
        Customer newCustomer = customerService.createNewCustomer(req);
        return new ResponseEntity<>(newCustomer, HttpStatus.CREATED);
    }

    @PostMapping("/find-by-phone")
    public ResponseEntity<Customer> findCustomerByPhone(@RequestBody CreateCustomerRequest request) throws Exception {
        String phoneNumber = request.getPhoneNumber();
        Customer customer = customerService.findCustomerByPhoneNumber(phoneNumber);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }
}
