package com.klu.dto;

import lombok.Data;

@Data
public class ProductRequest {
    private String name;
    private String category;
    private Integer price;
    private Integer stock;
    private String artisanName;
    private String region;
    private String description;
    private String culturalNote;
    private String imageUrl;
}
