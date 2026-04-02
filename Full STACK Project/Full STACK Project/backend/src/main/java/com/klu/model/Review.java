package com.klu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    private String id;

    private String productId;
    private String customerName;
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private String createdAt;
}
