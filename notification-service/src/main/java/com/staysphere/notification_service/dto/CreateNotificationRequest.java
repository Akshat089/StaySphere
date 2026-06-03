package com.staysphere.notification_service.dto;

import com.staysphere.notification_service.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    @NotNull
    private Long userId;

    @NotNull
    private NotificationType type;

    @NotBlank
    private String message;
}