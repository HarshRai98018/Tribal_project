package com.klu.controller;

import com.klu.dto.OrderRequest;
import com.klu.exception.ApiException;
import com.klu.model.*;
import com.klu.service.OrderService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody OrderRequest req,
                                                       @AuthenticationPrincipal User user) {
        if (!Set.of("customer", "admin").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(req, user));
    }

    @PatchMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id,
                              @RequestBody Map<String, String> body,
                              @AuthenticationPrincipal User user) {
        if (!Set.of("admin", "artisan").contains(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return orderService.updateStatus(id, body.get("status"));
    }
}
