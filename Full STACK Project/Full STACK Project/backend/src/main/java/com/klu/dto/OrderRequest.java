package com.klu.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private List<OrderItemReq> items;
    private String paymentMethod;
    private String paymentDetails;
    private String shippingAddress;

    @Data
    public static class OrderItemReq {
        private String productId;
        private Integer qty;
    }
}
