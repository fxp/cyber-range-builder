package com.cyberrange.pointsmall.service;

import com.cyberrange.pointsmall.cache.CacheService;
import com.cyberrange.pointsmall.crypto.CryptoService;
import com.cyberrange.pointsmall.model.User;
import com.cyberrange.pointsmall.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String USER_CACHE_KEY = "user:info:";
    private static final int USER_CACHE_TTL = 3600;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CryptoService cryptoService;

    @Transactional
    public User register(String username, String password, String email, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setPhone(phone);

        try {
            user.setSecretKey(cryptoService.generateAesKey());
        } catch (Exception e) {
            log.warn("Failed to generate secret key for user {}", username);
        }

        return userRepository.save(user);
    }

    public User getUserById(Long userId) {
        String cacheKey = USER_CACHE_KEY + userId;
        return cacheService.get(cacheKey, User.class)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("用户不存在"));
                    cacheService.set(cacheKey, user, USER_CACHE_TTL);
                    return user;
                });
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Transactional
    public User updateProfile(Long userId, String realName, String phone, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (realName != null) user.setRealName(realName);
        if (phone != null) user.setPhone(phone);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);

        User saved = userRepository.save(user);
        cacheService.delete(USER_CACHE_KEY + userId);
        return saved;
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        cacheService.delete(USER_CACHE_KEY + userId);
    }

    public void updateUserLevel(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        long totalPoints = user.getTotalPoints();

        User.UserLevel newLevel;
        if (totalPoints >= 100000) {
            newLevel = User.UserLevel.DIAMOND;
        } else if (totalPoints >= 50000) {
            newLevel = User.UserLevel.PLATINUM;
        } else if (totalPoints >= 20000) {
            newLevel = User.UserLevel.GOLD;
        } else if (totalPoints >= 5000) {
            newLevel = User.UserLevel.SILVER;
        } else {
            newLevel = User.UserLevel.BRONZE;
        }

        if (user.getLevel() != newLevel) {
            user.setLevel(newLevel);
            userRepository.save(user);
            cacheService.delete(USER_CACHE_KEY + userId);
            log.info("User {} level upgraded to {}", userId, newLevel);
        }
    }
}
