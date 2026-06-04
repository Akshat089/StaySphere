package com.staysphere.booking_service.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreatedEvent {

    private Long bookingId;
    private Long propertyId;
    private Long guestId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
}