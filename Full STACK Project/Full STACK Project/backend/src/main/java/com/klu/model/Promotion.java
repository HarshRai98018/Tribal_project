package com.klu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {
    @Id
    private String id;

    private String title;
    private Integer discountPercent;
    private String category;

    @Builder.Default
    private Boolean active = true;
}
