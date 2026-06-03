package com.staysphere.review_service.controller;

import com.staysphere.review_service.dto.CreateReviewRequest;
import com.staysphere.review_service.dto.ReviewResponse;
import com.staysphere.review_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ReviewResponse createReview(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.createReview(request);
    }

    @GetMapping("/{id}")
    public ReviewResponse getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id);
    }

    @GetMapping("/property/{propertyId}")
    public List<ReviewResponse> getReviewsByPropertyId(@PathVariable Long propertyId) {
        return reviewService.getReviewsByPropertyId(propertyId);
    }

    @GetMapping("/user/{userId}")
    public List<ReviewResponse> getReviewsByUserId(@PathVariable Long userId) {
        return reviewService.getReviewsByUserId(userId);
    }

    @DeleteMapping("/{id}")
    public String deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return "Review deleted successfully";
    }
}