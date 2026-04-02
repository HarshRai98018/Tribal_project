package com.klu.service;

import com.klu.exception.ApiException;
import com.klu.model.*;
import com.klu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    private static final Set<String> VALID_STATUSES = Set.of("pending", "success", "failed", "refunded");

    public PaymentService(PaymentRepository paymentRepo, OrderRepository orderRepo,
                          ProductRepository productRepo) {
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
    }

    @Transactional
    public Payment processPayment(String id, User user) {
        Payment payment = paymentRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));

        if ("customer".equals(user.getRole()) && !payment.getCustomerId().equals(user.getId())) {
            throw ApiException.forbidden("Payment does not belong to this user");
        }

        payment.setStatus("success");
        payment.setGatewayResponse("Demo payment approved");
        payment.setProcessedAt(Instant.now().toString());

        orderRepo.findById(payment.getOrderId()).ifPresent(order -> {
            if (!"delivered".equals(order.getStatus())) {
                order.setStatus("processing");
                orderRepo.save(order);
            }
        });

        return paymentRepo.save(payment);
    }

    public Map<String, Object> getInvoice(String id, User user) {
        Payment payment = paymentRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));

        boolean isOwner = payment.getCustomerId().equals(user.getId());
        boolean isAdmin = "admin".equals(user.getRole());
        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("Invoice access denied");
        }

        Order order = orderRepo.findById(payment.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Linked order not found"));

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product product = productRepo.findById(item.getProductId()).orElse(null);
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("productId", item.getProductId());
            itemMap.put("productName", product != null ? product.getName() : item.getProductId());
            itemMap.put("qty", item.getQty());
            itemMap.put("price", item.getPrice());
            itemMap.put("lineTotal", item.getQty() * item.getPrice());
            items.add(itemMap);
        }

        Map<String, Object> orderInfo = new LinkedHashMap<>();
        orderInfo.put("id", order.getId());
        orderInfo.put("customerName", order.getCustomerName());
        orderInfo.put("amount", order.getAmount());
        orderInfo.put("shippingAddress", order.getShippingAddress());
        orderInfo.put("status", order.getStatus());
        orderInfo.put("createdAt", order.getCreatedAt());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payment", payment);
        result.put("order", orderInfo);
        result.put("items", items);
        return result;
    }

    public Payment updateStatus(String id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw ApiException.badRequest("Invalid payment status");
        }

        Payment payment = paymentRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));

        payment.setStatus(status);
        return paymentRepo.save(payment);
    }
}
