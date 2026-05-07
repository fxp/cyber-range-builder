package com.cyberrange.pointsmall.service;

import com.cyberrange.pointsmall.kafka.PointsEventProducer;
import com.cyberrange.pointsmall.model.*;
import com.cyberrange.pointsmall.repository.OrderRepository;
import com.cyberrange.pointsmall.repository.ProductRepository;
import com.cyberrange.pointsmall.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private PointsEventProducer eventProducer;

    @Transactional
    public Order createOrder(Long userId, List<Map<String, Object>> items, String shippingAddress,
                             String receiverName, String receiverPhone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        long totalPoints = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(item.get("productId").toString());
            Integer quantity = Integer.valueOf(item.get("quantity").toString());

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品不存在: " + productId));

            if (product.getStatus() != Product.ProductStatus.ON_SALE) {
                throw new RuntimeException("商品已下架: " + product.getName());
            }

            if (product.getStock() < quantity) {
                throw new RuntimeException("库存不足: " + product.getName());
            }

            long itemTotal = product.getPointsPrice() * quantity;
            totalPoints += itemTotal;

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPoints(product.getPointsPrice());
            orderItem.setTotalPoints(itemTotal);
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getImageUrl());
            orderItems.add(orderItem);
        }

        if (user.getAvailablePoints() < totalPoints) {
            throw new RuntimeException("积分不足，需要 " + totalPoints + " 积分，当前拥有 " + user.getAvailablePoints() + " 积分");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUser(user);
        order.setTotalPoints(totalPoints);
        order.setShippingAddress(shippingAddress);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setStatus(Order.OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            Product product = item.getProduct();
            int updated = productRepository.decreaseStock(product.getId(), item.getQuantity());
            if (updated == 0) {
                throw new RuntimeException("库存扣减失败: " + product.getName());
            }
        }
        savedOrder.setItems(orderItems);

        boolean pointsDeducted = pointsService.spendPoints(userId, totalPoints, savedOrder.getOrderNo());
        if (!pointsDeducted) {
            throw new RuntimeException("积分扣减失败");
        }

        savedOrder.setStatus(Order.OrderStatus.PAID);
        savedOrder.setPaidAt(LocalDateTime.now());
        orderRepository.save(savedOrder);

        eventProducer.sendOrderCreatedEvent(userId, savedOrder.getOrderNo(), totalPoints);

        return savedOrder;
    }

    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    public Order getOrderDetail(Long userId, String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }

        return order;
    }

    @Transactional
    public Order completeOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        eventProducer.sendOrderCompletedEvent(order.getUser().getId(), orderNo);

        return order;
    }

    @Transactional
    public Order cancelOrder(Long userId, String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.PAID) {
            throw new RuntimeException("当前订单状态不可取消");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);

        pointsService.earnPoints(userId, order.getTotalPoints(),
                com.cyberrange.pointsmall.model.PointsRecord.PointsType.EARN_ADMIN, "订单取消退还积分，订单号: " + orderNo);

        return order;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PM" + timestamp + uuid;
    }
}
