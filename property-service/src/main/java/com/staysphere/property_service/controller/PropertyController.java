package com.staysphere.property_service.controller;

import com.staysphere.property_service.dto.CreatePropertyRequest;
import com.staysphere.property_service.dto.PropertyResponse;
import com.staysphere.property_service.dto.PropertySummaryResponse;
import com.staysphere.property_service.dto.UpdatePropertyRequest;
import com.staysphere.property_service.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello Property";
    }

    @PostMapping
    public PropertyResponse createProperty(@Valid @RequestBody CreatePropertyRequest request) {
        return propertyService.createProperty(request);
    }

    @GetMapping("/{id}")
    public PropertyResponse getPropertyById(@PathVariable Long id) {
        return propertyService.getPropertyById(id);
    }

    @GetMapping
    public List<PropertySummaryResponse> getAllProperties() {
        return propertyService.getAllProperties();
    }

    @GetMapping("/host/{hostId}")
    public List<PropertySummaryResponse> getPropertiesByHostId(@PathVariable Long hostId) {
        return propertyService.getPropertiesByHostId(hostId);
    }

    @GetMapping("/city/{city}")
    public List<PropertySummaryResponse> getPropertiesByCity(@PathVariable String city) {
        return propertyService.getPropertiesByCity(city);
    }

    @PutMapping("/{id}")
    public PropertyResponse updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        return propertyService.updateProperty(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return "Property deleted successfully";
    }
}