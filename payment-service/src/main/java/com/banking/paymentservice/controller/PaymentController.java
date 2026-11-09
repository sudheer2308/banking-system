package com.banking.paymentservice.controller;

import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.dto.PaymentRequest;
import com.banking.paymentservice.service.PaymentService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@Valid @RequestBody PaymentRequest paymentRequest) throws RazorpayException {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPaymentOrder(paymentRequest));
    }


    @PostMapping("/webhook")
    public ResponseEntity<String> hanfleWebhook(@RequestBody Map<String, Object> payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok("Webhook processed");
    }

}
