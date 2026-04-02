package com.klu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    private String id;

    private String orderId;
    private String customerId;
    private String customerName;

    @Column(nullable = false)
    private Integer amount;

    private String method;
    private String details;

    @Builder.Default
    private String status = "pending";

    private String transactionRef;
    private String createdAt;
    private String gatewayResponse;
    private String processedAt;
}
