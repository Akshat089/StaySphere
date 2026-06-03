package com.staysphere.booking_service.controller;

import com.staysphere.booking_service.dto.BookingResponse;
import com.staysphere.booking_service.dto.CreateBookingRequest;
import com.staysphere.booking_service.dto.UpdateBookingRequest;
import com.staysphere.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @GetMapping("/guest/{guestId}")
    public List<BookingResponse> getBookingsByGuestId(@PathVariable Long guestId) {
        return bookingService.getBookingsByGuestId(guestId);
    }

    @GetMapping("/property/{propertyId}")
    public List<BookingResponse> getBookingsByPropertyId(@PathVariable Long propertyId) {
        return bookingService.getBookingsByPropertyId(propertyId);
    }

    @DeleteMapping("/{id}")
    public String cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return "Booking cancelled successfully";
    }
    @PutMapping("/{id}")
    public BookingResponse updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        return bookingService.updateBooking(id, request);
    }
}