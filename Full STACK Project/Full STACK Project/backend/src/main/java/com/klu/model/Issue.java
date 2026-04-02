package com.klu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {
    @Id
    private String id;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    private String status = "open";
}
