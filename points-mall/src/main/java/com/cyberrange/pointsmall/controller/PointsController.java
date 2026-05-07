package com.cyberrange.pointsmall.controller;

import com.cyberrange.pointsmall.dto.ApiResponse;
import com.cyberrange.pointsmall.model.PointsRecord;
import com.cyberrange.pointsmall.model.User;
import com.cyberrange.pointsmall.service.PointsService;
import com.cyberrange.pointsmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/points")
public class PointsController {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private UserService userService;

    @GetMapping("/balance")
    public ApiResponse<Map<String, Object>> getBalance(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());

        Map<String, Object> balance = new HashMap<>();
        balance.put("availablePoints", user.getAvailablePoints());
        balance.put("totalPoints", user.getTotalPoints());
        balance.put("frozenPoints", user.getFrozenPoints());
        balance.put("level", user.getLevel());

        return ApiResponse.success(balance);
    }

    @PostMapping("/signin")
    public ApiResponse<Map<String, Object>> dailySignin(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());
        boolean success = pointsService.dailySignin(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "签到成功" : "今日已签到");

        return ApiResponse.success(result);
    }

    @GetMapping("/history")
    public ApiResponse<Page<PointsRecord>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(pointsService.getPointsHistory(user.getId(), pageRequest));
    }
}
