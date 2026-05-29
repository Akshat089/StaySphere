package com.staysphere.notification_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello Notification";
    }
}
