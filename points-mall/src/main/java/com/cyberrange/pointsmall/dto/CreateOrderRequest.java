package com.cyberrange.pointsmall.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateOrderRequest {

    @NotEmpty(message = "购物车不能为空")
    private List<Map<String, Object>> items;

    private String shippingAddress;

    private String receiverName;

    private String receiverPhone;
}
