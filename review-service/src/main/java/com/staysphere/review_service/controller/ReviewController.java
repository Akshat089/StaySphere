package com.staysphere.review_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello Review";
    }
}
