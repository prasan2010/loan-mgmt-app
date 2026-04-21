package com.loanapp.mapper;

import com.loanapp.dto.LoanResponse;
import com.loanapp.model.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(loan.getCustomer().getFullName())")
    LoanResponse toResponse(Loan loan);

    List<LoanResponse> toResponseList(List<Loan> loans);
}
