package com.klu.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private String productId;
    private Integer rating;
    private String comment;
}
