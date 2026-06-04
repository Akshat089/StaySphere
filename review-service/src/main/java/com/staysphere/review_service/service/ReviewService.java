package com.staysphere.review_service.service;

import com.staysphere.review_service.dto.*;
import com.staysphere.review_service.dto.PaymentClientResponse;
import com.staysphere.review_service.entity.Review;
import com.staysphere.review_service.exception.DuplicateReviewException;
import com.staysphere.review_service.exception.ReviewNotFoundException;
import com.staysphere.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;
    @Value("${BOOKING_SERVICE_URL:http://localhost:8084}")
    private String bookingServiceUrl;
    @Value("${PAYMENT_SERVICE_URL:http://localhost:8085}")
    private String paymentServiceUrl;
    @Value("${NOTIFICATION_SERVICE_URL:http://localhost:8087}")
    private String notificationServiceUrl;
    public ReviewResponse createReview(CreateReviewRequest request) {
        Long currentUserId = getCurrentUserId();
        validateBookingForReview(request,currentUserId);
        validateSuccessfulPayment(request.getBookingId());
        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new DuplicateReviewException("Review already exists for booking id: " + request.getBookingId());
        }

        Review review = Review.builder()
                .propertyId(request.getPropertyId())
                .userId(currentUserId)
                .bookingId(request.getBookingId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        Review savedReview = reviewRepository.save(review);
        sendReviewCreatedNotification(savedReview);
        return mapToResponse(savedReview);
    }

    public ReviewResponse getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + id));

        return mapToResponse(review);
    }

    public List<ReviewResponse> getReviewsByPropertyId(Long propertyId) {
        return reviewRepository.findByPropertyId(propertyId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + id));
        if (!isAdmin() && !review.getUserId().equals(getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to delete this review");
        }
        reviewRepository.delete(review);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .propertyId(review.getPropertyId())
                .userId(review.getUserId())
                .bookingId(review.getBookingId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
    private void validateBookingForReview(CreateReviewRequest request,Long currentUserId) {
        try {
            BookingClientResponse booking = restTemplate.getForObject(
                    bookingServiceUrl + "/api/bookings/" + request.getBookingId(),
                    BookingClientResponse.class
            );

            if (booking == null) {
                throw new RuntimeException("Booking not found with id: " + request.getBookingId());
            }

            if (!booking.getGuestId().equals(currentUserId)) {
                throw new RuntimeException("Review user does not match booking guest");
            }

            if (!booking.getPropertyId().equals(request.getPropertyId())) {
                throw new RuntimeException("Review property does not match booking property");
            }

            if (!"CONFIRMED".equals(booking.getStatus())) {
                throw new RuntimeException("Review can only be created for CONFIRMED bookings");
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unable to validate booking for review: " + ex.getMessage());
        }
    }
    private void validateSuccessfulPayment(Long bookingId) {
        try {
            com.staysphere.review_service.dto.PaymentClientResponse payment = restTemplate.getForObject(
                    paymentServiceUrl + "/api/payments/booking/" + bookingId,
                    PaymentClientResponse.class
            );

            if (payment == null) {
                throw new RuntimeException("Payment not found for booking id: " + bookingId);
            }

            if (!"SUCCESS".equals(payment.getPaymentStatus())) {
                throw new RuntimeException("Review can only be created after successful payment");
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unable to validate payment for review: " + ex.getMessage());
        }
    }
    private void sendReviewCreatedNotification(Review review) {
        try {
            CreateNotificationRequest notification = CreateNotificationRequest.builder()
                    .userId(review.getUserId())
                    .type("REVIEW_CREATED")
                    .message("Your review was created for property id: " + review.getPropertyId())
                    .build();

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications",
                    notification,
                    Object.class
            );

        } catch (Exception ex) {
            System.out.println("Notification service failed: " + ex.getMessage());
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
