package com.klu.service;

import com.klu.dto.ProductRequest;
import com.klu.exception.ApiException;
import com.klu.model.Product;
import com.klu.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository productRepo;

    private static final Set<String> VALID_AUTH = Set.of("approved", "pending", "review_requested", "rejected");

    public ProductService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public List<Product> findAll() {
        return productRepo.findAll();
    }

    public Product create(ProductRequest req, String userName, String ownerEmail) {
        if (req.getName() == null || req.getCategory() == null || req.getPrice() == null
                || req.getStock() == null || req.getRegion() == null || req.getDescription() == null) {
            throw ApiException.badRequest("Missing required fields");
        }

        String artisanName = (req.getArtisanName() != null && !req.getArtisanName().isBlank())
                ? req.getArtisanName() : userName;

        Product product = Product.builder()
                .id("P" + System.currentTimeMillis() % 1000000)
                .name(req.getName())
                .category(req.getCategory())
                .price(req.getPrice())
                .stock(req.getStock())
                .artisanId("A" + System.currentTimeMillis() % 1000000)
                .artisanName(artisanName)
                .ownerEmail(ownerEmail)
                .region(req.getRegion())
                .description(req.getDescription())
                .culturalNote(req.getCulturalNote() != null ? req.getCulturalNote() : "")
                .imageUrl(req.getImageUrl() != null && !req.getImageUrl().isBlank() ? req.getImageUrl()
                        : "https://images.unsplash.com/photo-1459908676235-d5f02a50184b?auto=format&fit=crop&w=1200&q=80")
                .authenticityStatus("pending")
                .rating(4.5)
                .build();

        return productRepo.save(product);
    }

    public Product findById(String id) {
        return productRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    public Product updateAuthenticity(String id, String status) {
        if (!VALID_AUTH.contains(status)) {
            throw ApiException.badRequest("Invalid authenticity status");
        }

        Product product = productRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        product.setAuthenticityStatus(status);
        return productRepo.save(product);
    }

    public Product updateImageUrl(String id, String imageUrl) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setImageUrl(imageUrl);
        return productRepo.save(product);
    }

    public Product delete(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        productRepo.delete(product);
        return product;
    }
}
