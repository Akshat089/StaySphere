package com.staysphere.payment_service.service;

import com.staysphere.payment_service.dto.CreatePaymentRequest;
import com.staysphere.payment_service.dto.PaymentResponse;
import com.staysphere.payment_service.entity.Payment;
import com.staysphere.payment_service.enums.PaymentStatus;
import com.staysphere.payment_service.exception.PaymentNotFoundException;
import com.staysphere.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
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
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (paymentRepository.existsByBookingId(request.getBookingId())) {
            throw new RuntimeException("Payment already exists for booking id: " + request.getBookingId());
        }
        BookingClientResponse booking = validateBookingForPayment(request);
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentProvider(request.getPaymentProvider())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        savedPayment.setProviderTransactionId("MOCK_TXN_" + UUID.randomUUID());
        savedPayment.setPaymentStatus(PaymentStatus.SUCCESS);

        Payment completedPayment = paymentRepository.save(savedPayment);
        sendPaymentSuccessNotification(completedPayment);
        return mapToResponse(completedPayment);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));

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
    private BookingClientResponse validateBookingForPayment(CreatePaymentRequest request) {
        try {
            BookingClientResponse booking = restTemplate.getForObject(
                    "http://localhost:8084/api/bookings/" + request.getBookingId(),
                    BookingClientResponse.class
            );

            if (booking == null) {
                throw new RuntimeException("Booking not found with id: " + request.getBookingId());
            }

            if (!booking.getGuestId().equals(request.getUserId())) {
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

    private void sendPaymentSuccessNotification(Payment payment) {
        try {
            CreateNotificationRequest notification = CreateNotificationRequest.builder()
                    .userId(payment.getUserId())
                    .type("PAYMENT_SUCCESS")
                    .message("Your payment was successful for booking id: " + payment.getBookingId())
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
}