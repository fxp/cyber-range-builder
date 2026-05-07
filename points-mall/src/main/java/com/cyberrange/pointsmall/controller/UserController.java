package com.cyberrange.pointsmall.controller;

import com.cyberrange.pointsmall.dto.ApiResponse;
import com.cyberrange.pointsmall.model.User;
import com.cyberrange.pointsmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName());

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("realName", user.getRealName());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("level", user.getLevel());
        profile.put("availablePoints", user.getAvailablePoints());
        profile.put("totalPoints", user.getTotalPoints());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("lastSigninAt", user.getLastSigninAt());

        return ApiResponse.success(profile);
    }

    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> updateProfile(
            @RequestBody Map<String, String> updates,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        User updated = userService.updateProfile(
                user.getId(),
                updates.get("realName"),
                updates.get("phone"),
                updates.get("avatarUrl")
        );

        Map<String, Object> result = new HashMap<>();
        result.put("id", updated.getId());
        result.put("realName", updated.getRealName());
        result.put("phone", updated.getPhone());
        result.put("avatarUrl", updated.getAvatarUrl());

        return ApiResponse.success("更新成功", result);
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName());
        userService.changePassword(user.getId(), request.get("oldPassword"), request.get("newPassword"));
        return ApiResponse.success("密码修改成功", null);
    }
}
