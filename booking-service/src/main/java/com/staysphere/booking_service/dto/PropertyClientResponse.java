package com.staysphere.booking_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyClientResponse {

    private Long id;
    private Long hostId;
    private String title;
    private String description;
    private String city;
    private String country;
    private String address;
    private BigDecimal pricePerNight;
    private String currency;
    private Integer maxGuests;
    private String propertyType;
    private String status;
    private String amenities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}