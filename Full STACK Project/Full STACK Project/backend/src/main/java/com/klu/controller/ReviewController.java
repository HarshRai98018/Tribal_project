package com.klu.controller;

import com.klu.dto.ReviewRequest;
import com.klu.exception.ApiException;
import com.klu.model.Review;
import com.klu.model.User;
import com.klu.service.ReviewService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<Review> create(@RequestBody ReviewRequest req,
                                         @AuthenticationPrincipal User user) {
        if (!Set.of("customer", "admin").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(req, user.getName()));
    }
}
