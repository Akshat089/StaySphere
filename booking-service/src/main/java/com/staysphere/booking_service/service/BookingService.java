package com.staysphere.booking_service.service;

import com.staysphere.booking_service.dto.*;
import com.staysphere.booking_service.entity.Booking;
import com.staysphere.booking_service.enums.BookingStatus;
import com.staysphere.booking_service.exception.BookingNotFoundException;
import com.staysphere.booking_service.exception.BookingOverlapException;
import com.staysphere.booking_service.exception.InvalidBookingDateException;
import com.staysphere.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        Long currentUserId = getCurrentUserId();

        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        validateGuestExists(currentUserId);
        validateProperty(request.getPropertyId());

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
                .guestId(currentUserId)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency())
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        sendBookingCreatedNotification(savedBooking);
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
        if (!isAdmin() && !booking.getGuestId().equals(getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to modify this booking");
        }
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
                .version(booking.getVersion())
                .build();
    }
    @Transactional
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        if (!isAdmin() && !booking.getGuestId().equals(getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to modify this booking");
        }
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
    private void validateProperty(Long propertyId) {
        try {
            PropertyClientResponse property = restTemplate.getForObject(
                    "http://localhost:8082/api/properties/" + propertyId,
                    PropertyClientResponse.class
            );

            if (property == null) {
                throw new RuntimeException("Property not found with id: " + propertyId);
            }

            if (!"ACTIVE".equals(property.getStatus())) {
                throw new RuntimeException("Property is not active and cannot be booked");
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unable to validate property: " + ex.getMessage());
        }
    }

    private void sendBookingCreatedNotification(Booking booking) {
        try {
            CreateNotificationRequest notification = CreateNotificationRequest.builder()
                    .userId(booking.getGuestId())
                    .type("BOOKING_CREATED")
                    .message("Your booking has been confirmed for property id: " + booking.getPropertyId())
                    .build();

            restTemplate.postForObject(
                    "http://localhost:8087/api/notifications",
                    notification,
                    Object.class
            );

        } catch (Exception ex) {
            System.out.println("Notification service failed: " + ex.getMessage());
        }
    }
    private void validateGuestExists(Long guestId) {
        try {
            UserClientResponse user = restTemplate.getForObject(
                    "http://localhost:8081/api/users/" + guestId,
                    UserClientResponse.class
            );

            if (user == null) {
                throw new RuntimeException("Guest user not found with id: " + guestId);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unable to validate guest user: " + ex.getMessage());
        }
    }
    private Long getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getDetails() == null) {
            throw new RuntimeException("Unauthorized: user details missing");
        }

        Object details = authentication.getDetails();

        if (details instanceof Long) {
            return (Long) details;
        }

        if (details instanceof Integer) {
            return ((Integer) details).longValue();
        }

        return Long.parseLong(details.toString());
    }

    private boolean isAdmin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}