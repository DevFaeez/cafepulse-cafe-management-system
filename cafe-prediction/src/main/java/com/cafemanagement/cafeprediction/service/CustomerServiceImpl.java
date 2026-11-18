package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.exception.CustomerNotFoundException;
import com.cafemanagement.cafeprediction.model.Customer;
import com.cafemanagement.cafeprediction.repository.CustomerRepository;
import com.cafemanagement.cafeprediction.request.CreateCustomerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private DailyOrderSequenceService sequences;
    @Override
    public Customer findCustomerById(Long id) throws Exception {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            throw new CustomerNotFoundException("customer not found by id...");
        }
        return customer.get();
    }

    @Override
    public Customer findCustomerByPhoneNumber(String phoneNumber) throws Exception {
        return customerRepository.findFirstByPhoneNumberOrderById(phoneNumber)
                .orElseThrow(() -> new CustomerNotFoundException("customer not found by phone number..."));
    }

    @Override
    public Optional<Customer> findByNormalizedPhone(String phone) {
        String normalized = normalizePhone(phone);
        return customerRepository.findFirstByPhoneNumberOrderById(normalized)
                .or(() -> customerRepository.findLegacyPhone(normalized));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public Customer createNewCustomer(CreateCustomerRequest req) {
        String phone = normalizePhone(req.getPhoneNumber());
        sequences.lockCheckout();
        return findByNormalizedPhone(phone).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setPhoneNumber(phone);
            return customerRepository.save(customer);
        });
    }

    static String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("[ ()-]", "");
        if (normalized.startsWith("+60")) {
            normalized = "0" + normalized.substring(3);
        }
        if (!normalized.matches("\\+?[0-9]{8,15}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid phone number.");
        }
        return normalized;
    }
}
