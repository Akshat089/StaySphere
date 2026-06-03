package com.staysphere.notification_service.controller;

import com.staysphere.notification_service.dto.CreateNotificationRequest;
import com.staysphere.notification_service.dto.NotificationResponse;
import com.staysphere.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public NotificationResponse createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.createNotification(request);
    }

    @GetMapping("/{id}")
    public NotificationResponse getNotificationById(@PathVariable Long id) {
        return notificationService.getNotificationById(id);
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getNotificationsByUserId(@PathVariable Long userId) {
        return notificationService.getNotificationsByUserId(userId);
    }

    @GetMapping("/user/{userId}/unread")
    public List<NotificationResponse> getUnreadNotificationsByUserId(@PathVariable Long userId) {
        return notificationService.getUnreadNotificationsByUserId(userId);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    public String deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return "Notification deleted successfully";
    }
}