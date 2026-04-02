package com.klu.repository;

import com.klu.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, String> {
    long countByActiveTrue();
}
