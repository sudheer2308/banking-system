package com.banking.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String otp = (String) payload.get("otp");
            String transactionId = (String) payload.get("transactionId");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(accountNumber,"TRANSACTION VERIFICATION REQUIRED",
                    String.format("Suspicious activity detected on your account " +
                            "Reason %s" +
                            "A Transaction of %s is pending verification " +
                            "your OTP is: %s valid for 5 minutes " +
                            "if this wasn't your ignore this message. ", reason, amount,otp
                    ));

        } catch (Exception ex) {
            log.error("Error sending Notification {}", ex.getMessage());
        }

    }

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload) {
        try {
            String senderAccount = (String) payload.get("senderAccountNumber");
            String receiverAccount = (String) payload.get("receiverAccountNumber");
            String amount = payload.get("amount").toString();
            //Debit Alert
            sendAlert(senderAccount, "DEBIT Alert", String.format("%s debited from account %s",
                    amount, senderAccount));
            //CREDIT Alert
            sendAlert(receiverAccount, "CREDIT ALERT", String.format("%s credited from account %s",
                    amount, receiverAccount));

        }catch (Exception ex) {
            log.error("Error sending transaction notification", ex.getMessage());
        }
    }

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            sendAlert(accountNumber, "SUSPICIOUS ACTIVITY DETECTED",
                    String.format("Your account %s has been blocked " +
                            "Reason %s " +
                            "Please contact your bank immediately", accountNumber, reason));
        }catch (Exception e) {
            log.error("Error sending fraud alert", e.getMessage());
        }
    }
    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(@Payload Map<String, Object> payload) {
        try {
            String senderAccount = (String) payload.get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(senderAccount, "REFUND PROCESSED",
                    String.format("Your transaction of %s was cancelled" +
                            "Reason : %s "+
                            "%s has been refunded to account %s", amount,reason,amount,senderAccount));

        } catch (Exception ex) {
            log.error("Error sending refund Notification", ex.getMessage());
        }
    }

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(accountNumber, "PAYMENT SUCCESSFUL",
                String.format("Payment of %s is completed" +
                                "Razorpay ID : %s",
                        amount, payload.get("razorpayPaymentId")));

        } catch (Exception ex) {
            log.error("Error sending payment Notification", ex.getMessage());

        }
    }
    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = (String) payload.get("amount");
            sendAlert(accountNumber, "PAYMENT FAILED",
                    String.format("Your payment of %s couldnt be processed" +
                            "please try again or contact support ", amount));
        } catch (Exception e) {
            log.error("Error sending payment failure Notification", e.getMessage());
        }
    }
    private void sendAlert(String accountNumber, String subject, String message) {
        log.info("-------------------------");

        log.info("Account {}", accountNumber);
        log.info("Subject: {} ", subject);
        log.info("Message: {} ", message);
        log.info("--------------------------------");
    }
}
