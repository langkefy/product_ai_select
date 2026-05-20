package com.select.product.controller;

import com.select.product.config.JwtUtil;
import com.select.product.dto.LoginDTO;
import com.select.product.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginDTO dto) {
        if (!adminUsername.equals(dto.getUsername()) || !adminPassword.equals(dto.getPassword())) {
            return Result.fail(401, "账号或密码错误");
        }
        String token = jwtUtil.generate(dto.getUsername());
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("username", dto.getUsername());
        return Result.ok(data);
    }
}
