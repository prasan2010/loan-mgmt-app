package com.loanapp.dto;

import com.loanapp.model.Customer;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerResponse {
    public Long id;
    public String firstName;
    public String lastName;
    public String fullName;
    public String email;
    public String phone;
    public LocalDate dateOfBirth;
    public String panNumber;
    public String address;
    public Integer creditScore;
    public Double annualIncome;
    public Customer.CustomerStatus status;
    public LocalDateTime createdAt;
    public int totalLoans;
}
