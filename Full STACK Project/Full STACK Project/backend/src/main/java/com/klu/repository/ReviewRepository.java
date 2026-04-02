package com.klu.repository;

import com.klu.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByProductId(String productId);
    List<Review> findAllByOrderByCreatedAtDesc();
}
