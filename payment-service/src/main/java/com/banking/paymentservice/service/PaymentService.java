package com.banking.paymentservice.service;

import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.dto.PaymentRequest;
import com.banking.paymentservice.entity.Payment;
import com.banking.paymentservice.entity.PaymentStatus;
import com.banking.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${razorpay.key-id}")
    private String keyId;
    @Value("${razorpay.key-secret}")
    private String keySecret;

    private static final String PAYMENT_COMPLETED_TOPIC= "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC= "payment.failed";

    /**
     * create razorPay payment order
     * flow
     *  1. create order in razorpay
     *  2. save paymeny record in DB
     *  3. Return order details to frontend
     *  4. frontend show RazorPay checkout
     *  5. user pays
     *  6. razorpay calls webhook
     * @param paymentRequest
     * @return
     */
    public PaymentOrderResponse createPaymentOrder(PaymentRequest paymentRequest) throws RazorpayException {
        log.info("creating paymeny order for account :{} amount {}", paymentRequest.getAccountNumber(),
                paymentRequest.getAmount());
        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
        // converted amount
        int convertedAmount = paymentRequest.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "USD/INR");
        orderRequest.put("receipt", "rcpt_"+ System.currentTimeMillis()+UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10));

        Order razorPayOrder = razorpayClient.orders.create(orderRequest);

        log.info("Razorpay order created {}", razorPayOrder.get("id").toString());

        // save payment record
        Payment payment = new Payment();
        payment.setRazorpayPaymentId(razorPayOrder.get("id").toString());
        payment.setAccountNumber(paymentRequest.getAccountNumber());
        payment.setAmount(paymentRequest.getAmount());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(paymentRequest.getDescription());

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentOrderResponse(savedPayment.getId(),
                razorPayOrder.get("id").toString(),"USD",
                paymentRequest.getAmount(), "CREATED", keyId);

    }
    public void handleWebhook(Map<String, Object> payload) {
    log.info("Received Razorpay webhook : {}", payload.get("event"));
       String event = (String) payload.get("event");

       if("payment.captured".equals(event)) {
           handlePaymenySuccess(payload);
       } else if("payment.failed".equals(event)) {
           handlePaymentFailed(payload);
       }
    }

    private void handlePaymenySuccess(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("order_id");
            String paymentId = (String) paymentData.get("id");
            Payment payment = paymentRepository.findByRazorpayOrderId(orderId).orElseThrow(() ->
                    new RuntimeException("Payment not found for order: {}"+ orderId));
            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            //publish to kafka
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber",payment.getAccountNumber());
            event.put("amount",payment.getAmount());
            event.put("razorpayPaymentId", payment.getRazorpayPaymentId());
            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, payment.getId(),event);
            log.info("Payment Completed:{}", payment.getId());
        } catch (Exception ex) {
            log.error("Error handling payment success ", ex.getMessage());

        }
    }

    private void handlePaymentFailed(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("order_id");
            Payment payment = paymentRepository.findByRazorpayOrderId(orderId).orElseThrow(() ->
                    new RuntimeException("Payment not found for order: {}"+ orderId));
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("payment failed via Razorpay");
            paymentRepository.save(payment);
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber",payment.getAccountNumber());
            event.put("amount",payment.getAmount());
            event.put("reason", "payment failed via Razorpay");
            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, payment.getId(),event);
            log.info("Payment failed :{}", payment.getId());

        } catch (Exception ex) {
            log.error("Error handling payment failure ", ex.getMessage());

        }
    }

    private Map<String, Object> extractPaymentData(Map<String, Object> payload) {
        Map<String, Object> entity = (Map<String,Object>) payload.get("payload");
        Map<String, Object> paymentWrapper =  (Map<String, Object>) entity.get("payment");

        return (Map<String, Object>) paymentWrapper.get("entity");

    }


}
