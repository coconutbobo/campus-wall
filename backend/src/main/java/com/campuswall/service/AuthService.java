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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
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

    // 密保问题的预设列表：注册时只能从这里面选，不能自己随便写，这样找回密码时问题和答案才对得上。
    // 前端注册页从 /api/auth/security-questions 接口拿这份列表，不要在前端另外硬编码一份。
    public static final List<String> SECURITY_QUESTIONS = List.of(
            "你母亲的姓名是？",
            "你出生的城市是？",
            "你小学班主任的姓名是？",
            "你最喜欢的动物是？",
            "你第一次养的宠物的名字是？"
    );

    // 注册：学号必须是 11 位数字，密保问题必须在预设列表里，答案必填；检查学号是否重复，找到校区，密码和密保答案都加密后保存，返回公开 ID 和令牌。
    // 昵称 30 天冷却从第一次改昵称开始算，注册本身不算改，所以新账号的 nicknameChangedAt 留空，第一次改昵称不受限制。
    public Map<String, Object> register(RegisterRequest request) {
        if (request.studentId() == null || !request.studentId().matches("\\d{11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId must be 11 digits");
        }
        if (request.securityQuestion() == null || !SECURITY_QUESTIONS.contains(request.securityQuestion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid security question");
        }
        String securityAnswer = request.securityAnswer() == null ? "" : request.securityAnswer().trim();
        if (securityAnswer.isEmpty() || securityAnswer.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "security answer must be 1-64 characters");
        }
        if (userRepository.existsByStudentId(request.studentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId already exists");
        }
        Campus campus = campusRepository.findById(request.campusId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "campus not found"));
        User user = new User();
        user.setStudentId(request.studentId());
        user.setNickname(request.nickname());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setSecurityQuestion(request.securityQuestion());
        user.setSecurityAnswerHash(passwordEncoder.encode(securityAnswer));
        user.setCampus(campus);
        userRepository.save(user);
        return Map.of("token", jwtUtil.generate(user.getPublicId(), user.getTokenVersion()), "publicId", user.getPublicId(), "nickname", user.getNickname(), "role", user.getRole().name(), "campusId", campus.getId(), "campusName", campus.getName());
    }

    // 按学号查密保问题：找回密码第一步，先让用户知道自己注册时选的是哪个问题。
    // 只返回问题本身，绝不返回答案和答案的加密值；账号不存在或已被注销时也不说出具体原因，只说账号不存在。
    public Map<String, Object> getSecurityQuestion(String studentId) {
        User user = userRepository.findByStudentId(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "account not found"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account not found");
        }
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "this account has no security question, please contact an admin");
        }
        return Map.of("securityQuestion", user.getSecurityQuestion());
    }

    // 忘密码重置：不用登录，凭学号加密保答案核对身份，答案对了才能设新密码。
    // 新密码 6 到 64 个字符；重置成功后令牌版本号递增，之前发出的所有令牌立刻失效，改完要重新登录。
    // 答案错了只说答案不正确，不透露注册时的问题和答案，避免别人靠报错猜答案。
    @Transactional
    public void resetPasswordBySecurity(String studentId, String securityAnswer, String newPassword) {
        User user = userRepository.findByStudentId(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "account not found"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account not found");
        }
        if (user.getSecurityQuestion() == null || user.getSecurityAnswerHash() == null || user.getSecurityAnswerHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "this account has no security question, please contact an admin");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "new password must be 6-64 characters");
        }
        String answer = securityAnswer == null ? "" : securityAnswer.trim();
        if (!passwordEncoder.matches(answer, user.getSecurityAnswerHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incorrect security answer");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
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

    // 我的资料：只返回公开信息，学号绝不经接口返回，连本人也不例外。
    // 另外返回下次可改昵称的日期：没改过昵称就是空，表示现在就能改；改过就按上次改的时间加 30 天算。
    public Map<String, Object> profile(String publicId) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("publicId", user.getPublicId());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole().name());
        result.put("campusName", user.getCampus() == null ? "" : user.getCampus().getName());
        result.put("nextNicknameChangeAt", nextNicknameChangeDate(user));
        return result;
    }

    // 下次可改昵称的日期：没改过返回空；上次改的时间加 30 天，如果已经过了 30 天也返回空，表示现在就能改。
    private String nextNicknameChangeDate(User user) {
        if (user.getNicknameChangedAt() == null) {
            return null;
        }
        LocalDate next = user.getNicknameChangedAt().toLocalDate().plusDays(30);
        if (!next.isAfter(LocalDate.now())) {
            return null;
        }
        return next.toString();
    }

    // 改资料：只改昵称，学号不给用户改，接口收到 studentId 也直接忽略，绝不改数据库里的学号。
    // 昵称必填，1 到 64 个字符；昵称 30 天只能改一次，改完这次要等满 30 天才能再改，冷却期内会返回下次可改日期。
    // 传的昵称和现在一样就不算改，不记时间也不触发冷却。
    // 校验失败都返回 400 而不是 401，因为前端拦截器一见 401 就会清令牌跳登录页，昵称输错不该把人踢下线。
    @Transactional
    public Map<String, Object> updateProfile(String publicId, String nickname) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        String newNickname = nickname == null ? "" : nickname.trim();
        if (newNickname.isEmpty() || newNickname.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname must be 1-64 characters");
        }
        if (newNickname.equals(user.getNickname())) {
            return profile(publicId);
        }
        if (user.getNicknameChangedAt() != null && user.getNicknameChangedAt().plusDays(30).isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname can only be changed once every 30 days, next change available on " + nextNicknameChangeDate(user));
        }
        user.setNickname(newNickname);
        user.setNicknameChangedAt(LocalDateTime.now());
        userRepository.save(user);
        return profile(publicId);
    }

    // 改密码：必须登录，必须先输对原密码，新密码 6 到 64 个字符。
    // 改完令牌版本号递增，之前发出的所有令牌立刻失效，所以前端改完要清本地令牌，让用户重新登录。
    // 校验失败返回 400 而不是 401，原密码输错不该把人踢下线。
    @Transactional
    public void changePassword(String publicId, String currentPassword, String newPassword) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "new password must be 6-64 characters");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
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
        user.setSecurityQuestion(null);
        user.setSecurityAnswerHash(null);
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
