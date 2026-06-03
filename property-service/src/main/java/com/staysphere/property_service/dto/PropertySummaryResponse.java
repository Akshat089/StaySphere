package com.staysphere.property_service.dto;
import com.staysphere.property_service.enums.PropertyStatus;
import com.staysphere.property_service.enums.PropertyType;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySummaryResponse {
    private Long id;
    private Long hostId;
    private String title;
    private String city;
    private String country;
    private Double pricePerNight;
    private String currency;
    private Integer maxGuests;
    private PropertyType propertyType;
    private PropertyStatus status;
}
