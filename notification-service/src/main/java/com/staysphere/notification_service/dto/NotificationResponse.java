package com.staysphere.notification_service.dto;

import com.staysphere.notification_service.enums.NotificationStatus;
import com.staysphere.notification_service.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String message;
    private NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}