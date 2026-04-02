package com.klu.controller;

import com.klu.dto.ProductRequest;
import com.klu.exception.ApiException;
import com.klu.model.Product;
import com.klu.model.User;
import com.klu.service.ProductService;
import com.klu.service.SupabaseStorageService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final SupabaseStorageService storageService;

    public ProductController(ProductService productService, SupabaseStorageService storageService) {
        this.productService = productService;
        this.storageService = storageService;
    }

    @GetMapping
    public List<Product> list() {
        return productService.findAll();
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductRequest req, @AuthenticationPrincipal User user) {
        if (!Set.of("artisan", "admin").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req, user.getName(), user.getEmail()));
    }

    @PatchMapping("/{id}/authenticity")
    public Product updateAuth(@PathVariable String id,
                              @RequestBody Map<String, String> body,
                              @AuthenticationPrincipal User user) {
        if (!Set.of("consultant", "admin").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return productService.updateAuthenticity(id, body.get("authenticityStatus"));
    }

    @PostMapping("/{id}/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@PathVariable String id,
                                                            @RequestParam("file") MultipartFile file,
                                                            @AuthenticationPrincipal User user) {
        if (!Set.of("artisan", "admin").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }

        try {
            String originalName = file.getOriginalFilename();
            String ext = ".jpg";
            if (originalName != null) {
                int idx = originalName.lastIndexOf('.');
                if (idx >= 0 && idx < originalName.length() - 1) {
                    ext = originalName.substring(idx);
                }
            }
            String fileName = id + ext;
            String publicUrl = storageService.uploadFile(file, fileName);
            productService.updateImageUrl(id, publicUrl);
            return ResponseEntity.ok(Map.of("imageUrl", publicUrl));
        } catch (Exception e) {
            throw ApiException.badRequest("Image upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id,
                                                       @AuthenticationPrincipal User user) {
        if (!Set.of("artisan", "admin").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }

        Product product = productService.findById(id);

        // Artisans can only delete their own products (checked by email, not display name)
        if ("artisan".equals(user.getRole()) &&
                product.getOwnerEmail() != null &&
                !user.getEmail().equals(product.getOwnerEmail())) {
            throw ApiException.forbidden("You can only delete your own products");
        }

        Product deleted = productService.delete(id);

        // Clean up image from Supabase Storage if it was uploaded there
        String imageUrl = deleted.getImageUrl();
        if (imageUrl != null && imageUrl.contains("supabase.co/storage/")) {
            try {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                storageService.deleteFile(fileName);
            } catch (Exception e) {
                System.err.println("Image cleanup warning: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("message", "Product deleted", "id", id));
    }
}
