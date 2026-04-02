package com.klu.repository;

import com.klu.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByAuthenticityStatusNot(String status);
    List<Product> findByStockLessThan(int threshold);
    List<Product> findByRatingGreaterThanEqual(double rating);
    long countByAuthenticityStatusNot(String status);
    long countByAuthenticityStatus(String status);
    long countByStockLessThan(int threshold);
    long countByRatingGreaterThanEqual(double threshold);
}
