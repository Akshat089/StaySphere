package com.staysphere.notification_service.service;

import com.staysphere.notification_service.dto.CreateNotificationRequest;
import com.staysphere.notification_service.dto.NotificationResponse;
import com.staysphere.notification_service.entity.Notification;
import com.staysphere.notification_service.enums.NotificationStatus;
import com.staysphere.notification_service.exception.NotificationNotFoundException;
import com.staysphere.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationResponse createNotification(CreateNotificationRequest request) {

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .message(request.getMessage())
                .status(NotificationStatus.SENT)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        return mapToResponse(savedNotification);
    }

    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        return mapToResponse(notification);
    }

    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        notification.setStatus(NotificationStatus.READ);

        Notification updatedNotification = notificationRepository.save(notification);

        return mapToResponse(updatedNotification);
    }

    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        notificationRepository.delete(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}