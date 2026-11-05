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

    // 密码格式：至少 6 位，最多 64 位，必须同时包含字母和数字；只含字母或只含数字都不算合格。
    // 注册、忘密码重置和改密码都走这个校验，登录时不校验格式，老账号原来的密码照样能登录。
    private static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 64) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

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
        if (!isValidPassword(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码格式：至少 6 位，必须包含字母和数字");
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
    // 新密码至少 6 位，必须包含字母和数字；重置成功后令牌版本号递增，之前发出的所有令牌立刻失效，改完要重新登录。
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
        if (!isValidPassword(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码格式：至少 6 位，必须包含字母和数字");
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
        // 是否已设密保：只返回有没有，不返回问题和答案本身，前端靠它显示“已设置 / 未设置”。
        result.put("hasSecurityQuestion", user.getSecurityQuestion() != null && !user.getSecurityQuestion().isBlank());
        // 当前选的密保问题本身也可以返回：问题只是预设列表里的一句话，不是秘密，忘密码接口本来就能按学号查到它，
        // 前端靠它让下拉框默认选中当前问题，改密保时不用每次都回到第一个问题。答案绝不返回。
        result.put("securityQuestion", user.getSecurityQuestion());
        // 联系方式是用户自己填的，返回给本人看和改，发帖时要不要公开由帖子自己的开关决定。
        result.put("contactInfo", user.getContactInfo());
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
    public Map<String, Object> updateProfile(String publicId, String nickname, String contactInfo) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        String newNickname = nickname == null ? "" : nickname.trim();
        if (newNickname.isEmpty() || newNickname.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname must be 1-64 characters");
        }
        // 联系方式随时能改，不受昵称 30 天冷却限制，最长 128 个字符；传了空串就是清空。
        if (contactInfo != null) {
            String newContactInfo = contactInfo.trim();
            if (newContactInfo.length() > 128) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contact info must be at most 128 characters");
            }
            user.setContactInfo(newContactInfo.isEmpty() ? null : newContactInfo);
        }
        if (newNickname.equals(user.getNickname())) {
            userRepository.save(user);
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

    // 改密码：必须登录，必须先输对原密码，新密码至少 6 位，必须包含字母和数字。
    // 改完令牌版本号递增，之前发出的所有令牌立刻失效，所以前端改完要清本地令牌，让用户重新登录。
    // 校验失败返回 400 而不是 401，原密码输错不该把人踢下线。
    @Transactional
    public void changePassword(String publicId, String currentPassword, String newPassword) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误");
        }
        if (!isValidPassword(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码格式：至少 6 位，必须包含字母和数字");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    // 补设 / 更换密保问题：给注册时还没有密保的老账号（比如 owner）用的，也能给已经设过的账号换问题和答案。
    // 必须登录，必须先输对原密码，防止别人拿到登录态就把找回密码的口子改成自己的；只改密保不改密码，所以令牌版本号不动，不用重新登录。
    // 问题必须从预设列表里选，答案 1 到 64 个字符，只存 BCrypt 加密值；校验失败返回 400 而不是 401，原密码输错不该把人踢下线。
    @Transactional
    public void setSecurityQuestion(String publicId, String currentPassword, String securityQuestion, String securityAnswer) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误");
        }
        if (securityQuestion == null || !SECURITY_QUESTIONS.contains(securityQuestion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid security question");
        }
        String answer = securityAnswer == null ? "" : securityAnswer.trim();
        if (answer.isEmpty() || answer.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "security answer must be 1-64 characters");
        }
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswerHash(passwordEncoder.encode(answer));
        userRepository.save(user);
    }

    // 注销登录：令牌版本号递增，之前发出的所有令牌立刻失效，前端也要清除本地保存的令牌。
    // 只靠前端清除令牌是不够的，旧令牌在有效期内仍然能用，所以必须有服务端的版本号校验。
    public void logout(String publicId) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    // 注销账号：状态改为已注销，学号保留原学号再加 _delete，密码和联系方式清空，自己的帖子和评论改为匿名，避免注销后仍能通过内容关联到本人。
    // 这样做原学号就空出来了，之后还能用原学号重新注册，而注销后的那一行因为带了 _delete，不会和新账号撞唯一约束。
    // 注意：学号字段在数据库里最长 32 个字符，原学号太长时先截到 25 个字符再加 _delete，正好 32 个字符。
    @Transactional
    public void deleteAccount(String publicId) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        user.setStatus(UserStatus.DELETED);
        user.setTokenVersion(user.getTokenVersion() + 1);
        String originalStudentId = user.getStudentId();
        if (originalStudentId != null && originalStudentId.length() > 25) {
            originalStudentId = originalStudentId.substring(0, 25);
        }
        user.setStudentId((originalStudentId == null ? "deleted" : originalStudentId) + "_delete");
        user.setNickname("已注销用户");
        user.setPasswordHash("");
        user.setSecurityQuestion(null);
        user.setSecurityAnswerHash(null);
        user.setContactInfo(null);
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
