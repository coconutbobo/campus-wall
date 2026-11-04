// 校园墙项目
// 账号服务：学号注册时检查学号是否重复并对密码做加密，登录时核对密码和账号状态，成功后返回令牌和用户信息。
package com.campuswall.service;

import com.campuswall.dto.LoginRequest;
import com.campuswall.dto.RegisterRequest;
import com.campuswall.entity.Campus;
import com.campuswall.entity.User;
import com.campuswall.entity.UserStatus;
import com.campuswall.repository.CampusRepository;
import com.campuswall.repository.UserRepository;
import com.campuswall.security.JwtUtil;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 注册新账号：先检查学号有没有被注册过，再把密码加密后保存。
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByStudentId(request.studentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId already exists");
        }
        Campus campus = campusRepository.findById(request.campusId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "campus not found"));
        User user = new User();
        user.setStudentId(request.studentId());
        user.setNickname(request.nickname());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCampus(campus);
        userRepository.save(user);
        return Map.of("token", jwtUtil.generate(user.getStudentId()), "studentId", user.getStudentId(), "nickname", user.getNickname(), "role", user.getRole().name(), "campusId", campus.getId(), "campusName", campus.getName());
    }

    // 登录：核对学号和密码，封禁账号直接拒绝，成功后返回令牌。
    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByStudentId(request.studentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        if (user.getStatus() == UserStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "account banned");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        String campusName = user.getCampus() == null ? "" : user.getCampus().getName();
        Long campusId = user.getCampus() == null ? null : user.getCampus().getId();
        return Map.of("token", jwtUtil.generate(user.getStudentId()), "studentId", user.getStudentId(), "nickname", user.getNickname(), "role", user.getRole().name(), "campusId", campusId == null ? 0L : campusId, "campusName", campusName);
    }
}
