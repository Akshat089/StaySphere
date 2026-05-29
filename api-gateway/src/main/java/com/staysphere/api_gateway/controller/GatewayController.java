package com.staysphere.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello Gateway";
    }
}
