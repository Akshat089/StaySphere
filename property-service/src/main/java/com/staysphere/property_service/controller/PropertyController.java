package com.staysphere.property_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PropertyController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello Property";
    }
}
