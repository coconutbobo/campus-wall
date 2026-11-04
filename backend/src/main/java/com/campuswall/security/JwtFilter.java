// 校园墙项目
// 令牌过滤器：每次请求先看请求头里有没有令牌，校验通过就查出用户并放行，封禁账号在这里直接拦截。
package com.campuswall.security;

import com.campuswall.entity.User;
import com.campuswall.entity.UserStatus;
import com.campuswall.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    // 每次请求都会先经过这里：从请求头取令牌，校验通过就把登录身份放进安全上下文。
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.valid(token)) {
                String studentId = jwtUtil.parseStudentId(token);
                User user = userRepository.findByStudentId(studentId).orElse(null);
                if (user != null && user.getStatus() == UserStatus.ACTIVE) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(studentId, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
