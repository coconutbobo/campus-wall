// 校园墙项目
// 账号接口：注册和登录两个接口，接收前端传来的学号、昵称和密码。
package com.campuswall.controller;

import com.campuswall.dto.LoginRequest;
import com.campuswall.dto.RegisterRequest;
import com.campuswall.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    // 注册新账号：先检查学号有没有被注册过，再把密码加密后保存。
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    // 登录：核对学号和密码，封禁账号直接拒绝，成功后返回令牌。
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
