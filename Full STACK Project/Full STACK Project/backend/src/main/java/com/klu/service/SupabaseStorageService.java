package com.klu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupabaseStorageService {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${app.supabase.bucket-name}")
    private String bucketName;

    private final RestTemplate restTemplate;

    public SupabaseStorageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Create the bucket if it does not exist. Safe to call multiple times. */
    private void ensureBucketExists() {
        String url = supabaseUrl + "/storage/v1/bucket";
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Check if bucket already exists by trying to get it
        try {
            HttpEntity<Void> getEntity = new HttpEntity<>(headers);
            restTemplate.exchange(url + "/" + bucketName, HttpMethod.GET, getEntity, String.class);
            return; // bucket exists, nothing to do
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() != 400 && e.getStatusCode().value() != 404) {
                System.err.println("Bucket check warning: " + e.getMessage());
            }
            // 400/404 means bucket not found — proceed to create
        } catch (Exception e) {
            System.err.println("Bucket check warning: " + e.getMessage());
        }

        // Create the bucket as public
        String body = "{\"id\":\"" + bucketName + "\",\"name\":\"" + bucketName + "\",\"public\":true}";
        HttpEntity<String> createEntity = new HttpEntity<>(body, headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, createEntity, String.class);
            System.out.println("Supabase bucket '" + bucketName + "' created successfully.");
        } catch (HttpClientErrorException e) {
            // 409 = already exists — fine
            if (e.getStatusCode().value() != 409) {
                System.err.println("Bucket creation failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            System.err.println("Bucket creation error: " + e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file, String fileName) throws Exception {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured.");
        }

        ensureBucketExists();

        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"));
        headers.set("x-upsert", "true");

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
            }
            throw new RuntimeException("Upload failed: " + response.getStatusCode() + " " + response.getBody());
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Upload failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    public void deleteFile(String fileName) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()) {
            return;
        }

        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceRoleKey);
        headers.set("Authorization", "Bearer " + serviceRoleKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            System.err.println("Warning: Failed to delete file from Supabase: " + e.getMessage());
        }
    }
}
