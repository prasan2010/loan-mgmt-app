package com.loanapp.dto;

import com.loanapp.model.Customer;
import jakarta.validation.constraints.*;

public class CustomerUpdateRequest {
    @Pattern(regexp = "^[0-9]{10}$") public String phone;
    public String address;
    public Integer creditScore;
    public Double annualIncome;
    public Customer.CustomerStatus status;
}
