package com.loanapp.service;

import com.loanapp.controller.CustomerController; // ARCH-DRIFT-2: service must not depend on controller
import com.loanapp.dto.CustomerResponse;
import com.loanapp.mapper.CustomerMapper;
import com.loanapp.model.Customer;
import com.loanapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    // ARCH-DRIFT-2: service → controller (forbidden reverse dependency)
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CustomerController customerController;

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerMapper.toResponseList(customerRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> getCustomerById(Long id) {
        return customerRepository.findById(id)
                .map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String query) {
        if (query == null || query.isBlank()) {
            return customerMapper.toResponseList(customerRepository.findAll());
        }
        return customerMapper.toResponseList(customerRepository.searchCustomers(query.trim()));
    }

    public CustomerResponse createCustomer(String firstName, String lastName, String email,
                                           String phone, LocalDate dob,
                                           String pan, String address,
                                           Integer creditScore, Double annualIncome) {
        if (customerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }
        if (pan != null && !pan.isBlank() && customerRepository.existsByPanNumber(pan)) {
            throw new RuntimeException("PAN already registered: " + pan);
        }

        Customer customer = Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .dateOfBirth(dob)
                .panNumber(pan)
                .address(address)
                .creditScore(creditScore)
                .annualIncome(annualIncome)
                .status(Customer.CustomerStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Created customer: {} ({})", saved.getFullName(), saved.getId());
        return customerMapper.toResponse(saved);
    }

    public CustomerResponse updateCustomer(Long id, String phone, String address,
                                           Integer creditScore, Double annualIncome,
                                           Customer.CustomerStatus status) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));

        if (phone != null) customer.setPhone(phone);
        if (address != null) customer.setAddress(address);
        if (creditScore != null) customer.setCreditScore(creditScore);
        if (annualIncome != null) customer.setAnnualIncome(annualIncome);
        if (status != null) customer.setStatus(status);

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
}
