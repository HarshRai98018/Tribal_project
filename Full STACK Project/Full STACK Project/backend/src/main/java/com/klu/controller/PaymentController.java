package com.klu.controller;

import com.klu.exception.ApiException;
import com.klu.model.Payment;
import com.klu.model.User;
import com.klu.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{id}/process")
    public Payment process(@PathVariable String id, @AuthenticationPrincipal User user) {
        if (!Set.of("customer", "admin").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return paymentService.processPayment(id, user);
    }

    @GetMapping("/{id}/invoice")
    public Map<String, Object> invoice(@PathVariable String id, @AuthenticationPrincipal User user) {
        return paymentService.getInvoice(id, user);
    }

    @PatchMapping("/{id}/status")
    public Payment updateStatus(@PathVariable String id,
                                @RequestBody Map<String, String> body,
                                @AuthenticationPrincipal User user) {
        if (!"admin".equals(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return paymentService.updateStatus(id, body.get("status"));
    }
}
