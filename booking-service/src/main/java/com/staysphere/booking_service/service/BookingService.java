package com.staysphere.booking_service.service;

import com.staysphere.booking_service.dto.BookingResponse;
import com.staysphere.booking_service.dto.CreateBookingRequest;
import com.staysphere.booking_service.dto.UpdateBookingRequest;
import com.staysphere.booking_service.entity.Booking;
import com.staysphere.booking_service.enums.BookingStatus;
import com.staysphere.booking_service.exception.BookingNotFoundException;
import com.staysphere.booking_service.exception.BookingOverlapException;
import com.staysphere.booking_service.exception.InvalidBookingDateException;
import com.staysphere.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {

        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        bookingRepository.lockProperty(request.getPropertyId());

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED
        );

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                request.getPropertyId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                blockingStatuses
        );

        if (!overlaps.isEmpty()) {
            throw new BookingOverlapException("Property is already booked for these dates");
        }

        Booking booking = Booking.builder()
                .propertyId(request.getPropertyId())
                .guestId(request.getGuestId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency())
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        return mapToResponse(savedBooking);
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        return mapToResponse(booking);
    }

    public List<BookingResponse> getBookingsByGuestId(Long guestId) {
        return bookingRepository.findByGuestId(guestId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<BookingResponse> getBookingsByPropertyId(Long propertyId) {
        return bookingRepository.findByPropertyId(propertyId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        booking.setStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);
    }

    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new InvalidBookingDateException("Check-out date must be after check-in date");
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new InvalidBookingDateException("Check-in date cannot be in the past");
        }
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .propertyId(booking.getPropertyId())
                .guestId(booking.getGuestId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
    @Transactional
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        bookingRepository.lockProperty(booking.getPropertyId());

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED
        );

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                booking.getPropertyId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                blockingStatuses
        );

        boolean hasOtherBookingOverlap = overlaps.stream()
                .anyMatch(existingBooking -> !existingBooking.getId().equals(id));

        if (hasOtherBookingOverlap) {
            throw new BookingOverlapException("Property is already booked for these dates");
        }

        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setTotalAmount(request.getTotalAmount());
        booking.setCurrency(request.getCurrency());

        Booking updatedBooking = bookingRepository.save(booking);

        return mapToResponse(updatedBooking);
    }
}