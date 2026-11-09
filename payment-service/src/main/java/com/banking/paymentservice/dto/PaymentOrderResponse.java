package com.banking.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderResponse {

    private String paymentId;
    private String razorpayOrderId;
    private String currency;
    private BigDecimal amount;
    private String status;
    private String razorpayKeyId;


}
