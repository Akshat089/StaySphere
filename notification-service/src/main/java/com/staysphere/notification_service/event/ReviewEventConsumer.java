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
public class ReviewEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.review-events}",
            groupId = "notification-service-group"
    )
    public void handleReviewCreated(String payload) {
        try {
            ReviewCreatedEvent event = objectMapper.readValue(payload, ReviewCreatedEvent.class);

            System.out.println("Received ReviewCreatedEvent for review id: " + event.getReviewId());

            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(event.getUserId())
                    .type(NotificationType.REVIEW_CREATED)
                    .message("Your review was created for property id: "
                            + event.getPropertyId()
                            + ". Review id: "
                            + event.getReviewId())
                    .build();

            notificationService.createNotification(request);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to consume ReviewCreatedEvent: " + ex.getMessage(), ex);
        }
    }
}