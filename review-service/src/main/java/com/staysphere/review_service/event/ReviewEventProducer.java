package com.staysphere.review_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staysphere.review_service.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.review-events}")
    private String reviewEventsTopic;

    public void publishReviewCreated(Review review) {
        try {
            ReviewCreatedEvent event = ReviewCreatedEvent.builder()
                    .reviewId(review.getId())
                    .propertyId(review.getPropertyId())
                    .userId(review.getUserId())
                    .bookingId(review.getBookingId())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    reviewEventsTopic,
                    String.valueOf(review.getId()),
                    payload
            );

            System.out.println("Published ReviewCreatedEvent: " + payload);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to publish ReviewCreatedEvent: " + ex.getMessage(), ex);
        }
    }
}