package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.UserResponse;
import com.ecommerce.auth.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Authenticated user", UserResponse.from(user)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<String>> dashboard(@AuthenticationPrincipal User user) {
        String message = "Welcome " + user.getFullName()
                + "! This is a protected endpoint. Only authenticated users can see this.";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}