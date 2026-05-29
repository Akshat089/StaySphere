package com.staysphere.user_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello User";
    }
}
