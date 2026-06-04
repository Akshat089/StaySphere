package com.staysphere.payment_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staysphere.payment_service.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-events}")
    private String paymentEventsTopic;

    public void publishPaymentSuccess(Payment payment) {
        try {
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .paymentId(payment.getId())
                    .bookingId(payment.getBookingId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .paymentStatus(payment.getPaymentStatus().name())
                    .paymentProvider(payment.getPaymentProvider().name())
                    .providerTransactionId(payment.getProviderTransactionId())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    paymentEventsTopic,
                    String.valueOf(payment.getId()),
                    payload
            );

            System.out.println("Published PaymentSuccessEvent: " + payload);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to publish PaymentSuccessEvent: " + ex.getMessage(), ex);
        }
    }
}