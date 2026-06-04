package com.staysphere.booking_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staysphere.booking_service.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.booking-events}")
    private String bookingEventsTopic;

    public void publishBookingCreated(Booking booking) {
        try {
            BookingCreatedEvent event = BookingCreatedEvent.builder()
                    .bookingId(booking.getId())
                    .propertyId(booking.getPropertyId())
                    .guestId(booking.getGuestId())
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .totalAmount(booking.getTotalAmount())
                    .currency(booking.getCurrency())
                    .status(booking.getStatus().name())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    bookingEventsTopic,
                    String.valueOf(booking.getId()),
                    payload
            );

            System.out.println("Published BookingCreatedEvent: " + payload);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to publish BookingCreatedEvent: " + ex.getMessage(), ex);
        }
    }
}