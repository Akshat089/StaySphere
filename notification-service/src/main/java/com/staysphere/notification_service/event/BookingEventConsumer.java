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
public class BookingEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.booking-events}",
            groupId = "notification-service-group"
    )
    public void handleBookingCreated(String payload) {
        try {
            BookingCreatedEvent event = objectMapper.readValue(payload, BookingCreatedEvent.class);

            System.out.println("Received BookingCreatedEvent for booking id: " + event.getBookingId());

            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(event.getGuestId())
                    .type(NotificationType.BOOKING_CREATED)
                    .message("Your booking has been confirmed for property id: "
                            + event.getPropertyId()
                            + ". Booking id: "
                            + event.getBookingId())
                    .build();

            notificationService.createNotification(request);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to consume BookingCreatedEvent: " + ex.getMessage(), ex);
        }
    }
}