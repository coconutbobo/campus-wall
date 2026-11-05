package com.campuswall.service;

// 账号服务：注册和登录只在内部核对学号，返回给前端的永远是公开 ID，绝不返回学号。
// 第一版不做学校系统核验，学号只是登录账号，不是已认证的身份证明，未来要做核验也要把它和公开展示分开做。
// 注销时学号会被替换成不可恢复的占位值，密码会被清空，自己的帖子和评论会改为匿名展示，避免注销后还能通过昵称关联到本人。
import com.campuswall.dto.LoginRequest;
import com.campuswall.dto.RegisterRequest;
import com.campuswall.entity.Campus;
import com.campuswall.entity.Post;
import com.campuswall.entity.Comment;
import com.campuswall.entity.User;
import com.campuswall.entity.UserStatus;
import com.campuswall.repository.CampusRepository;
import com.campuswall.repository.CommentRepository;
import com.campuswall.repository.PostRepository;
import com.campuswall.repository.UserRepository;
import com.campuswall.security.JwtUtil;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 注册：检查学号是否重复，找到校区，密码加密后保存，返回公开 ID 和令牌。
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
        return Map.of("token", jwtUtil.generate(user.getPublicId(), user.getTokenVersion()), "publicId", user.getPublicId(), "nickname", user.getNickname(), "role", user.getRole().name(), "campusId", campus.getId(), "campusName", campus.getName());
    }

    // 登录：先按学号找到账号，再核对密码和封禁状态，临时封禁到期后在这里自动恢复为正常账号。
    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByStudentId(request.studentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        if (user.isCurrentlyBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "account banned");
        }
        if (user.getStatus() == UserStatus.BANNED && !user.isCurrentlyBanned()) {
            user.setStatus(UserStatus.ACTIVE);
            user.setBanUntil(null);
            user.setBanReason(null);
            userRepository.save(user);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        String campusName = user.getCampus() == null ? "" : user.getCampus().getName();
        Long campusId = user.getCampus() == null ? null : user.getCampus().getId();
        return Map.of("token", jwtUtil.generate(user.getPublicId(), user.getTokenVersion()), "publicId", user.getPublicId(), "nickname", user.getNickname(), "role", user.getRole().name(), "campusId", campusId == null ? 0L : campusId, "campusName", campusName);
    }

    // 注销登录：令牌版本号递增，之前发出的所有令牌立刻失效，前端也要清除本地保存的令牌。
    // 只靠前端清除令牌是不够的，旧令牌在有效期内仍然能用，所以必须有服务端的版本号校验。
    public void logout(String publicId) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    // 注销账号：状态改为已注销，学号替换成占位值，密码清空，自己的帖子和评论改为匿名，避免注销后仍能通过内容关联到本人。
    // 注意：学号字段在数据库里最长 32 个字符，之前用的 "deleted_" 加完整 UUID 一共 44 个字符，存不进去会直接报错回滚，
    // 前端收到的就是失败，本地令牌也不会清，看起来就像点了注销却没注销。这里改用数据库主键做占位值，又短又唯一，也推不出原学号。
    @Transactional
    public void deleteAccount(String publicId) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        user.setStatus(UserStatus.DELETED);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setStudentId("deleted_" + user.getId());
        user.setNickname("已注销用户");
        user.setPasswordHash("");
        user.setBanUntil(null);
        user.setBanReason(null);
        userRepository.save(user);
        for (Post post : postRepository.findByAuthorId(user.getId())) {
            if (!post.isAnonymous()) {
                post.setAnonymous(true);
                postRepository.save(post);
            }
        }
        for (Comment comment : commentRepository.findByAuthorId(user.getId())) {
            if (!comment.isAnonymous()) {
                comment.setAnonymous(true);
                commentRepository.save(comment);
            }
        }
    }
}
