package com.staysphere.property_service.dto;


import com.staysphere.property_service.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePropertyRequest{
    @NotNull(message = "Host ID is required")
    private Long hostId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Address is required")
    private String address;

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
    @NotBlank(message = "Amenities is required")
    private String amenities;
}