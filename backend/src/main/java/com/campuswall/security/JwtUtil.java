// 校园墙项目
// 令牌工具：用配置里的密钥生成和校验 JWT，登录成功后发给前端，前端之后每次请求都要带上它。
package com.campuswall.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = Keys.hmacShaKeyFor(digest);
        } catch (Exception e) {
            throw new IllegalStateException("jwt key init failed", e);
        }
        this.expirationMs = expirationMs;
    }

    // 生成令牌：把学号放进令牌主体，并设置过期时间。
    public String generate(String studentId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder().subject(studentId).issuedAt(now).expiration(expiry).signWith(key).compact();
    }

    // 从令牌里解析出学号，令牌被篡改或过期会在这里校验失败。
    public String parseStudentId(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    // 校验令牌是否有效：能正常解析出来就是有效，解析失败就是无效。
    public boolean valid(String token) {
        try {
            parseStudentId(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
