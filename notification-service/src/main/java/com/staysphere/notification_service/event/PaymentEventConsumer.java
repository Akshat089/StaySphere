package com.staysphere.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staysphere.notification_service.dto.CreateNotificationRequest;
import com.staysphere.notification_service.enums.NotificationType;
import com.staysphere.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.payment-events}",
            groupId = "notification-service-group"
    )
    public void handlePaymentSuccess(String payload) {
        try {
            PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);

            System.out.println("Received PaymentSuccessEvent for payment id: " + event.getPaymentId());

            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(event.getUserId())
                    .type(NotificationType.PAYMENT_SUCCESS)
                    .message("Your payment was successful for booking id: "
                            + event.getBookingId()
                            + ". Payment id: "
                            + event.getPaymentId())
                    .build();

            notificationService.createNotification(request);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to consume PaymentSuccessEvent: " + ex.getMessage(), ex);
        }
    }
}