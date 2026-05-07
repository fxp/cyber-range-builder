package com.cyberrange.pointsmall.service;

import com.cyberrange.pointsmall.cache.CacheService;
import com.cyberrange.pointsmall.kafka.PointsEventProducer;
import com.cyberrange.pointsmall.model.PointsRecord;
import com.cyberrange.pointsmall.model.User;
import com.cyberrange.pointsmall.repository.PointsRecordRepository;
import com.cyberrange.pointsmall.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);
    private static final String POINTS_CACHE_KEY = "user:points:";
    private static final String SIGNIN_LOCK_KEY = "signin:lock:";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointsRecordRepository pointsRecordRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PointsEventProducer eventProducer;

    @Value("${points.earn.signin-bonus:5}")
    private long signinBonus;

    @Value("${points.earn.purchase-rate:10}")
    private int purchaseRate;

    @Value("${points.expire.days:365}")
    private int expireDays;

    @Transactional
    public boolean earnPoints(Long userId, Long points, PointsRecord.PointsType type, String description) {
        int updated = userRepository.addPoints(userId, points);
        if (updated == 0) {
            log.warn("Failed to add points for user {}", userId);
            return false;
        }

        User user = userRepository.findById(userId).orElseThrow();

        PointsRecord record = new PointsRecord();
        record.setUser(user);
        record.setPoints(points);
        record.setBalanceAfter(user.getAvailablePoints());
        record.setType(type);
        record.setDescription(description);
        record.setExpireAt(LocalDateTime.now().plusDays(expireDays));
        pointsRecordRepository.save(record);

        cacheService.delete(POINTS_CACHE_KEY + userId);

        eventProducer.sendPointsEarnedEvent(userId, points, type.name());
        return true;
    }

    @Transactional
    public boolean spendPoints(Long userId, Long points, String orderNo) {
        int updated = userRepository.deductPoints(userId, points);
        if (updated == 0) {
            log.warn("Insufficient points for user {}, required: {}", userId, points);
            return false;
        }

        User user = userRepository.findById(userId).orElseThrow();

        PointsRecord record = new PointsRecord();
        record.setUser(user);
        record.setPoints(-points);
        record.setBalanceAfter(user.getAvailablePoints());
        record.setType(PointsRecord.PointsType.SPEND_EXCHANGE);
        record.setDescription("积分兑换商品，订单号: " + orderNo);
        record.setSourceType("ORDER");
        pointsRecordRepository.save(record);

        cacheService.delete(POINTS_CACHE_KEY + userId);

        eventProducer.sendPointsSpentEvent(userId, points, orderNo);
        return true;
    }

    @Transactional
    public boolean dailySignin(Long userId) {
        String lockKey = SIGNIN_LOCK_KEY + userId + ":" + java.time.LocalDate.now();
        if (!cacheService.setNx(lockKey, "1", 86400)) {
            log.info("User {} already signed in today", userId);
            return false;
        }

        User user = userRepository.findById(userId).orElseThrow();
        user.setLastSigninAt(LocalDateTime.now());
        userRepository.save(user);

        return earnPoints(userId, signinBonus, PointsRecord.PointsType.EARN_SIGNIN, "每日签到奖励");
    }

    public Long getAvailablePoints(Long userId) {
        String cacheKey = POINTS_CACHE_KEY + userId;
        String cached = cacheService.hget(cacheKey, "available");
        if (cached != null) {
            return Long.parseLong(cached);
        }

        User user = userRepository.findById(userId).orElseThrow();
        cacheService.hset(cacheKey, "available", String.valueOf(user.getAvailablePoints()));
        return user.getAvailablePoints();
    }

    public Page<PointsRecord> getPointsHistory(Long userId, Pageable pageable) {
        return pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long calculatePurchasePoints(long amount) {
        return amount / purchaseRate;
    }
}
