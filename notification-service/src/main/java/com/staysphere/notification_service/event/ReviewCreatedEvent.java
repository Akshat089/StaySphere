package com.staysphere.notification_service.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreatedEvent {

    private Long reviewId;
    private Long propertyId;
    private Long userId;
    private Long bookingId;
    private Integer rating;
    private String comment;
}