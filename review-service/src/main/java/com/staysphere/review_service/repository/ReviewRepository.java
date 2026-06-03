package com.staysphere.review_service.repository;

import com.staysphere.review_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPropertyId(Long propertyId);

    List<Review> findByUserId(Long userId);

    boolean existsByBookingId(Long bookingId);
}