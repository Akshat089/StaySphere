package com.staysphere.search_service.dto;

import com.staysphere.search_service.enums.PropertyType;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySearchResponse implements Serializable {

    private Long propertyId;

    private String title;

    private String city;

    private String country;

    private Double pricePerNight;

    private String currency;

    private Integer maxGuests;

    private PropertyType propertyType;

    private String amenities;
}