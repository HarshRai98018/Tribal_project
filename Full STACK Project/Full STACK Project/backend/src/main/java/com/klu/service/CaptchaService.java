package com.klu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaService {

    @Value("${app.recaptcha.enabled:false}")
    private boolean enabled;

    @Value("${app.recaptcha.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate;

    public CaptchaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public boolean verify(String captchaToken) {
        if (!enabled) {
            return true;
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            return false;
        }

        String url = "https://www.google.com/recaptcha/api/siteverify?secret="
                + secretKey + "&response=" + captchaToken;

        try {
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            return false;
        }
    }
}
