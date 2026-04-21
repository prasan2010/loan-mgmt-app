package com.loanapp.mapper;

import com.loanapp.dto.PaymentResponse;
import com.loanapp.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "loanId", source = "loan.id")
    @Mapping(target = "loanNumber", source = "loan.loanNumber")
    PaymentResponse toResponse(Payment payment);

    List<PaymentResponse> toResponseList(List<Payment> payments);
}
