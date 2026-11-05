package com.campuswall.controller;

// 账号接口：注册、登录和注销，返回给前端的永远是公开 ID，绝不返回学号。
import com.campuswall.dto.LoginRequest;
import com.campuswall.dto.RegisterRequest;
import com.campuswall.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // 注册：学号只在内部核对，返回公开 ID 和令牌。
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // 登录：核对学号和密码，成功后返回公开 ID 和令牌。
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 密保问题列表：注册页用的预设问题，公开接口，前端不要自己硬编码一份。
    @GetMapping("/security-questions")
    public Map<String, Object> securityQuestions() {
        return Map.of("questions", AuthService.SECURITY_QUESTIONS);
    }

    // 按学号查密保问题：忘密码第一步，只返回问题，不返回答案。
    @GetMapping("/security-question")
    public Map<String, Object> securityQuestion(@RequestParam String studentId) {
        return authService.getSecurityQuestion(studentId);
    }

    // 忘密码重置：不用登录，传学号、密保答案和新密码，答案对了才能改，改完旧令牌全部失效，要重新登录。
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, Object> body) {
        authService.resetPasswordBySecurity(
                body.get("studentId") == null ? "" : String.valueOf(body.get("studentId")),
                body.get("securityAnswer") == null ? "" : String.valueOf(body.get("securityAnswer")),
                body.get("newPassword") == null ? "" : String.valueOf(body.get("newPassword")));
        return Map.of("ok", true);
    }

    // 注销登录：服务端令牌版本号递增，旧令牌立刻失效，前端也要清除本地令牌。
    @PostMapping("/logout")
    public Map<String, Object> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return Map.of("ok", true);
    }

    // 注销当前账号：学号会被替换成占位值，密码会被清空，自己的帖子和评论会改为匿名展示。
    @DeleteMapping("/me")
    public Map<String, Object> deleteAccount(Authentication authentication) {
        authService.deleteAccount(authentication.getName());
        return Map.of("ok", true);
    }
}
