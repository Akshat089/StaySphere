package com.staysphere.payment_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello Payment";
    }
}
