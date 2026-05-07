package com.cyberrange.pointsmall.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "points_records")
public class PointsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long points;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointsType type;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(length = 500)
    private String description;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "operator", length = 50)
    private String operator;

    @Column(name = "remark", length = 500)
    private String remark;

    public enum PointsType {
        EARN_PURCHASE,
        EARN_REVIEW,
        EARN_SIGNIN,
        EARN_ACTIVITY,
        EARN_ADMIN,
        SPEND_EXCHANGE,
        SPEND_EXPIRE,
        SPEND_ADMIN,
        FREEZE,
        UNFREEZE,
        TRANSFER_IN,
        TRANSFER_OUT
    }
}
