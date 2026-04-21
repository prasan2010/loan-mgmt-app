package com.loanapp.service;

import com.loanapp.model.Customer;
import com.loanapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> searchCustomers(String query) {
        if (query == null || query.isBlank()) {
            return customerRepository.findAll();
        }
        return customerRepository.searchCustomers(query.trim());
    }

    public Customer createCustomer(String firstName, String lastName, String email,
                                   String phone, java.time.LocalDate dob,
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
        return saved;
    }

    public Customer updateCustomer(Long id, String phone, String address,
                                   Integer creditScore, Double annualIncome,
                                   Customer.CustomerStatus status) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));

        if (phone != null) customer.setPhone(phone);
        if (address != null) customer.setAddress(address);
        if (creditScore != null) customer.setCreditScore(creditScore);
        if (annualIncome != null) customer.setAnnualIncome(annualIncome);
        if (status != null) customer.setStatus(status);

        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
}
