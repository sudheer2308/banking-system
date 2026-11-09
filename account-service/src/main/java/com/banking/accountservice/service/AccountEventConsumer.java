package com.banking.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.rmi.server.ExportException;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    /**
     * consume transaction completed event from kafka
     * credits receiver amount
     * @param payload
     */
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload) {
        try {
            String receiverAccount = (String) payload.get("receiverAccountNumber");
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            log.info("Creding account : {} amount : {}", receiverAccount , amount);

            accountService.creditBalance(receiverAccount, amount);
        } catch (Exception ex) {
            log.error("Error Crediting account : {}" , ex.getMessage());
        }
    }

    /**
     * consume fraud-detection event from kafka
     * blocks the flagged account
     * @param payload
     */
    @KafkaListener(topics = "fraud-detected")
    public void consumeFraudDetected(@Payload Map<String, Object> payload){
        try {
            String accountNumber = (String)payload.get("accountNUmber");
            log.info("Fraud detected - blocking account {}", accountNumber);

            accountService.blockAccount(accountNumber);
        } catch (Exception e) {
            log.error("Error Blocking account : {}", e.getMessage());
        }
    }
}
