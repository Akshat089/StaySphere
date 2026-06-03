package com.staysphere.booking_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    private Long userId;
    private String type;
    private String message;
}