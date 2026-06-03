package com.staysphere.payment_service.repository;

import com.staysphere.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
    List<Payment> findByUserId(Long userId);
}