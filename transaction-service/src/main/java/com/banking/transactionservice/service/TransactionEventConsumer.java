package com.banking.transactionservice.service;


import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import javax.swing.tree.TreeNode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;

    private final RedisTemplate<String, String> redisTemplate;

    private final TransactionService transactionService;
    private static final long OTP_EXPIRY_MINUTES = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC="transaction.otp.generated";
    /**
     * Consume verification required
     * generate OTP and aks user to verify
     * @param payload
     */
    @KafkaListener(topics = "verification.required")
    public void verificationRequired(@Payload Map<String, Object> payload) {
        try {
            String transactionId = (String) payload.get("transactionId");
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            log.info("Verification required - transactoiin: {} reason; {}", transactionId, reason);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("transaction not Found"+ transactionId));
        // idempotency gaurd inorder to protect two time
            if(transaction.getStatus() != TransactionStatus.PROCESSING) {
                log.warn("Transaction {} not processing", transactionId);
                return;
            }
            //generate 6 digit otp
            String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);
            // store otp in redis expires in 5 minutes
            String otpKey = "verification:otp"+transactionId;
            redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            // update status
            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);

            log.info("OTP Generated for transaction: {} expires in {} min ", transactionId, OTP_EXPIRY_MINUTES);

            // notify user  -> publish an event this will consume by notification service
            Map<String, Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId", transactionId);
            otpEvent.put("accountNumber", accountNumber);
            otpEvent.put("reason", reason);
            otpEvent.put("otp", otp);
            otpEvent.put("amount", payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId, otpEvent);


        } catch (Exception ex) {
            log.error("Error handling verification required {} ", ex.getMessage());

        }
    }

    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanResult(@Payload Map<String, Object> payload) {
        try {
            String transactionId = (String) payload.get("transactionId");
            transactionService.processCleanResult(transactionId);
        } catch (Exception ex) {
            log.error("Error Processing fraud check result ", ex.getMessage());

        }
    }
}
