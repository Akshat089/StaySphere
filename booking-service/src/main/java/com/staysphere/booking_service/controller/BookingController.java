package com.staysphere.booking_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello Booking";
    }
}
