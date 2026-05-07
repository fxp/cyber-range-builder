package com.cyberrange.pointsmall.controller;

import com.cyberrange.pointsmall.dto.ApiResponse;
import com.cyberrange.pointsmall.model.Product;
import com.cyberrange.pointsmall.service.OrderService;
import com.cyberrange.pointsmall.service.PointsService;
import com.cyberrange.pointsmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PointsService pointsService;

    @PostMapping("/products")
    public ApiResponse<Product> createProduct(@RequestBody Product product) {
        return ApiResponse.success(productService.createProduct(product));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ApiResponse.success(productService.updateProduct(id, product));
    }

    @PostMapping("/orders/{orderNo}/complete")
    public ApiResponse<Object> completeOrder(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.completeOrder(orderNo));
    }

    @PostMapping("/points/grant")
    public ApiResponse<Map<String, Object>> grantPoints(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long points = Long.valueOf(request.get("points").toString());
        String description = (String) request.get("description");

        boolean success = pointsService.earnPoints(
                userId, points,
                com.cyberrange.pointsmall.model.PointsRecord.PointsType.EARN_ADMIN,
                description != null ? description : "管理员赠送积分"
        );

        return ApiResponse.success(Map.of("success", success, "userId", userId, "points", points));
    }
}
