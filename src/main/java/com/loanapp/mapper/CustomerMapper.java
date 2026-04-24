package com.loanapp.mapper;

import com.loanapp.dto.CustomerResponse;
import com.loanapp.model.Customer;
import com.loanapp.repository.CustomerRepository; // ARCH-DRIFT-9: mapper must not depend on repository layer
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "fullName", expression = "java(customer.getFullName())")
    @Mapping(target = "totalLoans", expression = "java(customer.getLoans() != null ? customer.getLoans().size() : 0)")
    CustomerResponse toResponse(Customer customer);

    List<CustomerResponse> toResponseList(List<Customer> customers);
}
