package com.klu.service;

import com.klu.dto.ReviewRequest;
import com.klu.exception.ApiException;
import com.klu.model.Product;
import com.klu.model.Review;
import com.klu.repository.ProductRepository;
import com.klu.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;

    public ReviewService(ReviewRepository reviewRepo, ProductRepository productRepo) {
        this.reviewRepo = reviewRepo;
        this.productRepo = productRepo;
    }

    public Review create(ReviewRequest req, String customerName) {
        if (req.getProductId() == null || req.getRating() == null || req.getComment() == null) {
            throw ApiException.badRequest("Missing review fields");
        }

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        Review review = Review.builder()
                .id("R" + System.currentTimeMillis() % 1000000)
                .productId(req.getProductId())
                .customerName(customerName)
                .rating(req.getRating())
                .comment(req.getComment())
                .createdAt(LocalDate.now().toString())
                .build();

        reviewRepo.save(review);

        List<Review> productReviews = reviewRepo.findByProductId(req.getProductId());
        double avg = productReviews.stream().mapToInt(Review::getRating).average().orElse(4.5);
        product.setRating(Math.round(avg * 10.0) / 10.0);
        productRepo.save(product);

        return review;
    }
}
