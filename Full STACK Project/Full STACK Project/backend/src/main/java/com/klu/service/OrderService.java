package com.klu.service;

import com.klu.dto.OrderRequest;
import com.klu.exception.ApiException;
import com.klu.model.*;
import com.klu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;

    private static final Set<String> VALID_STATUSES = Set.of("confirmed", "processing", "shipped", "delivered", "cancelled");
    private static final Set<String> ALLOWED_METHODS = Set.of("upi", "card", "netbanking", "cod");

    public OrderService(OrderRepository orderRepo, ProductRepository productRepo,
                        PaymentRepository paymentRepo, UserRepository userRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Map<String, Object> placeOrder(OrderRequest req, User user) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw ApiException.badRequest("Invalid order payload");
        }
        if (req.getPaymentMethod() == null || !ALLOWED_METHODS.contains(req.getPaymentMethod())) {
            throw ApiException.badRequest("Invalid payment method");
        }
        String shippingAddress = req.getShippingAddress() != null ? req.getShippingAddress().trim() : "";
        if (shippingAddress.isBlank()) {
            throw ApiException.badRequest("Shipping address is required");
        }

        String orderId = "O" + System.currentTimeMillis() % 1000000;
        int amount = 0;
        List<OrderItem> items = new ArrayList<>();

        for (OrderRequest.OrderItemReq itemReq : req.getItems()) {
            Product product = productRepo.findById(itemReq.getProductId())
                    .orElseThrow(() -> ApiException.badRequest("Product " + itemReq.getProductId() + " does not exist"));

            int qty = itemReq.getQty() != null ? itemReq.getQty() : 1;
            if (qty <= 0 || qty > product.getStock()) {
                throw ApiException.badRequest("Invalid quantity for " + product.getName()
                        + ". Available stock: " + product.getStock());
            }

            product.setStock(product.getStock() - qty);
            productRepo.save(product);

            amount += product.getPrice() * qty;
            items.add(OrderItem.builder()
                    .productId(product.getId())
                    .qty(qty)
                    .price(product.getPrice())
                    .build());
        }

        Order order = Order.builder()
                .id(orderId)
                .customerId(user.getId())
                .customerName(user.getName())
                .amount(amount)
                .shippingAddress(shippingAddress)
                .status("confirmed")
                .createdAt(LocalDate.now().toString())
                .build();

        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);
        orderRepo.save(order);

        String txnRef = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = Payment.builder()
                .id("PAY" + System.currentTimeMillis() % 1000000)
                .orderId(orderId)
                .customerId(user.getId())
                .customerName(user.getName())
                .amount(amount)
                .method(req.getPaymentMethod())
                .details(req.getPaymentDetails() != null ? req.getPaymentDetails() : "")
                .status("pending")
                .transactionRef(txnRef)
                .createdAt(LocalDate.now().toString())
                .build();

        paymentRepo.save(payment);

        user.setSavedAddress(shippingAddress);
        userRepo.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("payment", payment);
        result.put("user", sanitizeUser(user));
        return result;
    }

    public Order updateStatus(String id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw ApiException.badRequest("Invalid status");
        }

        Order order = orderRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        order.setStatus(status);
        return orderRepo.save(order);
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("savedAddress", user.getSavedAddress());
        return result;
    }
}
