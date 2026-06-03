package com.staysphere.review_service.service;

import com.staysphere.review_service.dto.CreateReviewRequest;
import com.staysphere.review_service.dto.ReviewResponse;
import com.staysphere.review_service.entity.Review;
import com.staysphere.review_service.exception.DuplicateReviewException;
import com.staysphere.review_service.exception.ReviewNotFoundException;
import com.staysphere.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewResponse createReview(CreateReviewRequest request) {

        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new DuplicateReviewException("Review already exists for booking id: " + request.getBookingId());
        }

        Review review = Review.builder()
                .propertyId(request.getPropertyId())
                .userId(request.getUserId())
                .bookingId(request.getBookingId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);

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
}