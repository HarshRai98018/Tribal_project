package com.klu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer stock;

    private String artisanId;
    private String artisanName;
    private String ownerEmail;   // email of the user who created this product
    private String region;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String culturalNote;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Builder.Default
    private String authenticityStatus = "pending";

    @Builder.Default
    private Double rating = 4.5;
}
