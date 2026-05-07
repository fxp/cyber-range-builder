package com.cyberrange.pointsmall.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_price", nullable = false)
    private Long pointsPrice;

    @Column(name = "cash_price", precision = 10, scale = 2)
    private BigDecimal cashPrice;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "sold_count", nullable = false)
    private Integer soldCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ON_SALE;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "detail_images", columnDefinition = "TEXT")
    private String detailImages;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "min_level")
    @Enumerated(EnumType.STRING)
    private User.UserLevel minLevel = User.UserLevel.BRONZE;

    @Column(name = "exchange_limit")
    private Integer exchangeLimit = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    public enum ProductCategory {
        VIRTUAL,
        PHYSICAL,
        COUPON,
        EXPERIENCE,
        CHARITY
    }

    public enum ProductStatus {
        ON_SALE, OFF_SALE, SOLD_OUT, EXPIRED
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
