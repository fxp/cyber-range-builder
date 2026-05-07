package com.cyberrange.pointsmall.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "total_points", nullable = false)
    private Long totalPoints = 0L;

    @Column(name = "available_points", nullable = false)
    private Long availablePoints = 0L;

    @Column(name = "frozen_points", nullable = false)
    private Long frozenPoints = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserLevel level = UserLevel.BRONZE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_signin_at")
    private LocalDateTime lastSigninAt;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "secret_key")
    private String secretKey;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PointsRecord> pointsRecords = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    public enum UserStatus {
        ACTIVE, INACTIVE, BANNED
    }

    public enum UserLevel {
        BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
