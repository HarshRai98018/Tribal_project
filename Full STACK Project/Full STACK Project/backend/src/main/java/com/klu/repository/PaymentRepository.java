package com.klu.repository;

import com.klu.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByCustomerId(String customerId);
    long countByStatus(String status);
    List<Payment> findAllByOrderByCreatedAtDesc();
}
