package com.campuswall.security;

// 令牌工具：令牌里只放公开 ID，不放学号，因为令牌内容只是编码不是加密，任何人拿到令牌都能解码看到里面的字段。
// 密钥先做一次哈希再使用，这样即使配置里的密钥比较短，也能得到足够长的签名密钥。
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

    // 生成令牌：主体是公开 ID，不是学号，同时把令牌版本号放进声明里，注销后旧令牌版本号对不上就会失效。
    // 令牌有效期在配置里，默认 24 小时，解析时会自动校验过期时间，过期令牌会被拒绝。
    public String generate(String publicId, int tokenVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder().subject(publicId).claim("ver", tokenVersion).issuedAt(now).expiration(expiry).signWith(key).compact();
    }

    // 从令牌里解析出公开 ID。
    public String parsePublicId(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    // 从令牌里解析出令牌版本号，旧令牌没有这个声明时返回 -1，过滤器会把它当作失效处理。
    public int parseTokenVersion(String token) {
        Object version = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("ver");
        if (version instanceof Number) {
            return ((Number) version).intValue();
        }
        return -1;
    }

    // 校验令牌是否有效：能正常解析出来就是有效，解析失败就是无效。
    public boolean valid(String token) {
        try {
            parsePublicId(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
