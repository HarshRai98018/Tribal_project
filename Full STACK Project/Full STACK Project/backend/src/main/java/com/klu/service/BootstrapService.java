package com.klu.service;

import com.klu.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BootstrapService {

    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final ReviewRepository reviewRepo;
    private final PromotionRepository promoRepo;
    private final IssueRepository issueRepo;
    private final PaymentRepository paymentRepo;

    public BootstrapService(ProductRepository productRepo, OrderRepository orderRepo,
                            ReviewRepository reviewRepo, PromotionRepository promoRepo,
                            IssueRepository issueRepo, PaymentRepository paymentRepo) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.reviewRepo = reviewRepo;
        this.promoRepo = promoRepo;
        this.issueRepo = issueRepo;
        this.paymentRepo = paymentRepo;
    }

    public Map<String, Object> getAll() {
        var products = productRepo.findAll();
        var orders = orderRepo.findAll();
        var reviews = reviewRepo.findAll();
        var promotions = promoRepo.findAll();
        var issues = issueRepo.findAll();
        var payments = paymentRepo.findAll();

        long totalRevenue = orders.stream().mapToLong(o -> o.getAmount()).sum();
        long openIssues = issueRepo.countByStatus("open");
        long successPayments = paymentRepo.countByStatus("success");
        long pendingPayments = paymentRepo.countByStatus("pending");
        long pendingAuth = productRepo.countByAuthenticityStatusNot("approved");

        Map<String, Object> adminMetrics = new LinkedHashMap<>();
        adminMetrics.put("totalProducts", products.size());
        adminMetrics.put("totalOrders", orders.size());
        adminMetrics.put("totalRevenue", totalRevenue);
        adminMetrics.put("openIssues", openIssues);
        adminMetrics.put("successfulPayments", successPayments);

        Map<String, Object> artisanMetrics = new LinkedHashMap<>();
        artisanMetrics.put("totalListings", products.size());
        artisanMetrics.put("lowStock", productRepo.countByStockLessThan(10));
        artisanMetrics.put("pendingOrders", orderRepo.countByStatusNot("delivered"));

        Map<String, Object> customerMetrics = new LinkedHashMap<>();
        customerMetrics.put("activePromotions", promoRepo.countByActiveTrue());
        customerMetrics.put("topRatedProducts", productRepo.countByRatingGreaterThanEqual(4.7));
        customerMetrics.put("pendingPayments", pendingPayments);

        Map<String, Object> consultantMetrics = new LinkedHashMap<>();
        consultantMetrics.put("pendingAuthenticity", pendingAuth);
        consultantMetrics.put("approvedProducts", productRepo.countByAuthenticityStatus("approved"));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("admin", adminMetrics);
        metrics.put("artisan", artisanMetrics);
        metrics.put("customer", customerMetrics);
        metrics.put("consultant", consultantMetrics);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("products", products);
        result.put("orders", orders);
        result.put("reviews", reviews);
        result.put("promotions", promotions);
        result.put("issues", issues);
        result.put("payments", payments);
        result.put("metrics", metrics);
        result.put("activityLogs", buildActivityLogs(products, orders, payments, issues));

        return result;
    }

    private List<Map<String, Object>> buildActivityLogs(List<?> productsRaw, List<?> ordersRaw, List<?> paymentsRaw, List<?> issuesRaw) {
        @SuppressWarnings("unchecked")
        var products = (List<com.klu.model.Product>) productsRaw;
        @SuppressWarnings("unchecked")
        var orders = (List<com.klu.model.Order>) ordersRaw;
        @SuppressWarnings("unchecked")
        var payments = (List<com.klu.model.Payment>) paymentsRaw;
        @SuppressWarnings("unchecked")
        var issues = (List<com.klu.model.Issue>) issuesRaw;

        List<Map<String, Object>> logs = new ArrayList<>();

        for (var order : orders) {
            logs.add(activity("order", "Order " + order.getId(), order.getCustomerName(),
                    order.getStatus(), order.getCreatedAt()));
        }

        for (var payment : payments) {
            String detail = payment.getMethod() == null ? payment.getCustomerName()
                    : payment.getCustomerName() + " via " + payment.getMethod().toUpperCase();
            logs.add(activity("payment", "Payment " + payment.getId(), detail,
                    payment.getStatus(), payment.getProcessedAt() != null ? payment.getProcessedAt() : payment.getCreatedAt()));
        }

        for (var issue : issues) {
            logs.add(activity("issue", "Issue " + issue.getId(), issue.getType(),
                    issue.getStatus(), null));
        }

        for (var product : products) {
            logs.add(activity("product", product.getName(), product.getArtisanName(),
                    product.getAuthenticityStatus(), null));
        }

        logs.sort(Comparator
                .comparing((Map<String, Object> log) -> String.valueOf(log.getOrDefault("timestamp", "")))
                .reversed()
                .thenComparing(log -> String.valueOf(log.getOrDefault("title", ""))));

        return logs.stream().limit(18).toList();
    }

    private Map<String, Object> activity(String type, String title, String detail, String status, String timestamp) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("type", type);
        log.put("title", title == null ? "" : title);
        log.put("detail", detail == null ? "" : detail);
        log.put("status", status == null ? "" : status);
        log.put("timestamp", timestamp == null ? "" : timestamp);
        return log;
    }
}
