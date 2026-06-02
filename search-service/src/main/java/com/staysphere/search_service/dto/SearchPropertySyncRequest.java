package com.staysphere.search_service.dto;
import com.staysphere.search_service.enums.PropertyStatus;
import com.staysphere.search_service.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchPropertySyncRequest {
    @NotNull(message = "Property ID is required")
    private Long propertyId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Price per night is required")
    @Positive(message = "Price per night must be positive")
    private Double pricePerNight;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Max guests is required")
    @Positive(message = "Max guests must be positive")
    private Integer maxGuests;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotNull(message = "Property status is required")
    private PropertyStatus status;

    private String amenities;
}
