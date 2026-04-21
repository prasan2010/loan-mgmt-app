package com.loanapp.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CustomerRequest {
    @NotBlank public String firstName;
    @NotBlank public String lastName;
    @Email @NotBlank public String email;
    @Pattern(regexp = "^[0-9]{10}$") public String phone;
    @NotNull public LocalDate dateOfBirth;
    public String panNumber;
    public String address;
    public Integer creditScore;
    public Double annualIncome;
}
