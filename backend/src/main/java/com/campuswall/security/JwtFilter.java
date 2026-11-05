package com.campuswall.security;

// 令牌过滤器：每次请求先看请求头里有没有令牌，校验通过就查出用户并放行。
// 封禁判断在这里做：永久封禁直接拦截，临时封禁到期后当作正常账号放行，已注销账号也直接拦截。
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.valid(token)) {
                String publicId = jwtUtil.parsePublicId(token);
                User user = userRepository.findByPublicId(publicId).orElse(null);
                // 令牌版本号必须和当前账号的版本号一致，注销或封禁后递增版本号，旧令牌即使没过期也会在这里被拒绝。
                if (user != null && user.getTokenVersion() == jwtUtil.parseTokenVersion(token) && user.getStatus() != UserStatus.DELETED && !user.isCurrentlyBanned()) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(publicId, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
