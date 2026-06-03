package com.staysphere.booking_service.dto;

import com.staysphere.booking_service.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;

    private Long propertyId;

    private Long guestId;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private BigDecimal totalAmount;

    private String currency;

    private BookingStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long version;
}