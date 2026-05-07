package com.cyberrange.pointsmall.controller;

import com.cyberrange.pointsmall.dto.ApiResponse;
import com.cyberrange.pointsmall.dto.CreateOrderRequest;
import com.cyberrange.pointsmall.model.Order;
import com.cyberrange.pointsmall.model.User;
import com.cyberrange.pointsmall.service.OrderService;
import com.cyberrange.pointsmall.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ApiResponse<Order> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                          Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        Order order = orderService.createOrder(
                user.getId(),
                request.getItems(),
                request.getShippingAddress(),
                request.getReceiverName(),
                request.getReceiverPhone()
        );
        return ApiResponse.success("订单创建成功", order);
    }

    @GetMapping
    public ApiResponse<Page<Order>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(orderService.getUserOrders(user.getId(), pageRequest));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<Order> getOrder(@PathVariable String orderNo, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return ApiResponse.success(orderService.getOrderDetail(user.getId(), orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<Order> cancelOrder(@PathVariable String orderNo, Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        return ApiResponse.success("订单已取消", orderService.cancelOrder(user.getId(), orderNo));
    }
}
