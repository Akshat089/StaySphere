package com.staysphere.payment_service.service;

import com.staysphere.payment_service.dto.CreatePaymentRequest;
import com.staysphere.payment_service.dto.PaymentResponse;
import com.staysphere.payment_service.entity.Payment;
import com.staysphere.payment_service.enums.PaymentStatus;
import com.staysphere.payment_service.event.PaymentEventProducer;
import com.staysphere.payment_service.exception.PaymentNotFoundException;
import com.staysphere.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.staysphere.payment_service.dto.BookingClientResponse;
import com.staysphere.payment_service.dto.CreateNotificationRequest;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final PaymentEventProducer paymentEventProducer;
    @Value("${BOOKING_SERVICE_URL:http://localhost:8084}")
    private String bookingServiceUrl;
    @Value("${NOTIFICATION_SERVICE_URL:http://localhost:8087}")
    private String notificationServiceUrl;
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (paymentRepository.existsByBookingId(request.getBookingId())) {
            throw new RuntimeException("Payment already exists for booking id: " + request.getBookingId());
        }
        Long currentUserId = getCurrentUserId();

        BookingClientResponse booking = validateBookingForPayment(request, currentUserId);
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(currentUserId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentProvider(request.getPaymentProvider())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        savedPayment.setProviderTransactionId("MOCK_TXN_" + UUID.randomUUID());
        savedPayment.setPaymentStatus(PaymentStatus.SUCCESS);

        Payment completedPayment = paymentRepository.save(savedPayment);
        paymentEventProducer.publishPaymentSuccess(completedPayment);
        return mapToResponse(completedPayment);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        if (!isAdmin() && !payment.getUserId().equals(getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to access this payment");
        }
        return mapToResponse(payment);
    }

    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking id: " + bookingId));

        return mapToResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse refundPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        if (!isAdmin() && !payment.getUserId().equals(getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to access this payment");
        }
        payment.setPaymentStatus(PaymentStatus.REFUNDED);

        Payment refundedPayment = paymentRepository.save(payment);

        return mapToResponse(refundedPayment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentStatus(payment.getPaymentStatus())
                .paymentProvider(payment.getPaymentProvider())
                .providerTransactionId(payment.getProviderTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
    private BookingClientResponse validateBookingForPayment(CreatePaymentRequest request, Long currentUserId) {
        try {
            BookingClientResponse booking = restTemplate.getForObject(
                    bookingServiceUrl + "/api/bookings/" + request.getBookingId(),
                    BookingClientResponse.class
            );

            if (booking == null) {
                throw new RuntimeException("Booking not found with id: " + request.getBookingId());
            }

            if (!booking.getGuestId().equals(currentUserId)) {
                throw new RuntimeException("Payment user does not match booking guest");
            }

            if (!"CONFIRMED".equals(booking.getStatus())) {
                throw new RuntimeException("Payment can only be made for CONFIRMED bookings");
            }

            if (booking.getTotalAmount().compareTo(request.getAmount()) != 0) {
                throw new RuntimeException("Payment amount does not match booking amount");
            }

            if (!booking.getCurrency().equals(request.getCurrency())) {
                throw new RuntimeException("Payment currency does not match booking currency");
            }

            return booking;

        } catch (Exception ex) {
            throw new RuntimeException("Unable to validate booking for payment: " + ex.getMessage());
        }
    }

//    private void sendPaymentSuccessNotification(Payment payment) {
//        try {
//            CreateNotificationRequest notification = CreateNotificationRequest.builder()
//                    .userId(payment.getUserId())
//                    .type("PAYMENT_SUCCESS")
//                    .message("Your payment was successful for booking id: " + payment.getBookingId())
//                    .build();
//
//            restTemplate.postForObject(
//                    notificationServiceUrl + "/api/notifications",
//                    notification,
//                    Object.class
//            );
//
//        } catch (Exception ex) {
//            System.out.println("Notification service failed: " + ex.getMessage());
//        }
//    }
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
