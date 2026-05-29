package com.staysphere.search_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    @GetMapping(path = "/hello")
    public String hello() {
        return "Hello Service";
    }
}
