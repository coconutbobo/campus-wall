package com.campuswall.controller;

// 管理后台接口：只有管理员角色能调用，普通用户调用会直接被安全配置拦截。
// 管理端列表里不返回学号，管理员要看学号必须走单独的、带理由的身份查看接口，那次查看会被记进审计日志。
// 删帖、删评论、封禁、解封、处理举报这几类操作都必须留审计日志，理由会一并保存。
import com.campuswall.entity.Comment;
import com.campuswall.entity.Post;
import com.campuswall.entity.PostStatus;
import com.campuswall.entity.Report;
import com.campuswall.entity.User;
import com.campuswall.entity.UserStatus;
import com.campuswall.repository.AdminAuditLogRepository;
import com.campuswall.repository.CommentRepository;
import com.campuswall.repository.PostRepository;
import com.campuswall.repository.ReportRepository;
import com.campuswall.repository.UserRepository;
import com.campuswall.service.AdminAuditService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@Transactional
@RequiredArgsConstructor
public class AdminController {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminAuditService adminAuditService;

    private String reasonOf(Map<String, Object> body) {
        if (body == null || body.get("reason") == null) {
            return "未填写理由";
        }
        return String.valueOf(body.get("reason"));
    }

    // 管理端列表统一做分页，防止一次返回全部数据，默认 20 条，最多 50 条。
    private <T> org.springframework.data.domain.Page<T> paginate(java.util.List<T> all, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new org.springframework.data.domain.PageImpl<>(all.subList(from, to), org.springframework.data.domain.PageRequest.of(safePage, safeSize), all.size());
    }

    // 管理端帖子列表：匿名帖子在这里会带上作者公开 ID，方便管理员追责，但这个接口只有管理员能调用，公开接口不会这样返回。
    @GetMapping("/posts")
    public org.springframework.data.domain.Page<Map<String, Object>> posts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return paginate(postRepository.findAll().stream().map(post -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", post.getId());
            item.put("title", post.getTitle());
            item.put("category", post.getCategory().name());
            item.put("status", post.getStatus().name());
            item.put("deletedBy", post.getDeletedBy());
            item.put("anonymous", post.isAnonymous());
            item.put("displayName", post.isAnonymous() ? "匿名用户" : post.getAuthor().getNickname());
            item.put("authorPublicId", post.getAuthor().getPublicId());
            item.put("createdAt", post.getCreatedAt() == null ? null : post.getCreatedAt().toString());
            return item;
        }).collect(Collectors.toList()), page, size);
    }

    // 管理员删帖：只改状态为已删除，并记审计日志。
    @DeleteMapping("/posts/{id}")
    public Map<String, Object> deletePost(Authentication authentication, @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        post.setStatus(PostStatus.DELETED);
        post.setDeletedBy("ADMIN");
        postRepository.save(post);
        adminAuditService.log(authentication.getName(), "DELETE_POST", "POST", id, reasonOf(body));
        return Map.of("ok", true);
    }

    // 管理员删评论：只改状态为已删除，并记审计日志。
    @DeleteMapping("/comments/{id}")
    public Map<String, Object> deleteComment(Authentication authentication, @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));
        comment.setStatus("DELETED");
        commentRepository.save(comment);
        adminAuditService.log(authentication.getName(), "DELETE_COMMENT", "COMMENT", id, reasonOf(body));
        return Map.of("ok", true);
    }

    // 管理端用户列表：不返回学号，只返回公开 ID、昵称、角色、封禁状态和校区。
    @GetMapping("/users")
    public org.springframework.data.domain.Page<Map<String, Object>> users(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return paginate(userRepository.findAll().stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("publicId", user.getPublicId());
            item.put("nickname", user.getNickname());
            item.put("role", user.getRole().name());
            item.put("status", user.getStatus().name());
            item.put("banUntil", user.getBanUntil() == null ? null : user.getBanUntil().toString());
            item.put("banReason", user.getBanReason());
            item.put("campusName", user.getCampus() == null ? "" : user.getCampus().getName());
            return item;
        }).collect(Collectors.toList()), page, size);
    }

    // 管理员查看单个用户的学号：必须传理由，这次查看会被记进审计日志，普通列表里看不到学号。
    // 注销过的用户也能查：注销时学号被改成了原学号加 _delete，这里把后缀去掉再返回，管理员看到的就是原学号。
    // 学号是 11 位数字，正常学号绝不会以 _delete 结尾，所以去掉后缀不会误伤。
    @GetMapping("/users/{publicId}/identity")
    public Map<String, Object> viewIdentity(Authentication authentication, @PathVariable String publicId, @RequestParam(defaultValue = "未填写理由") String reason) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        adminAuditService.log(authentication.getName(), "VIEW_IDENTITY", "USER", user.getId(), reason);
        String studentId = user.getStudentId();
        if (user.getStatus() == UserStatus.DELETED && studentId != null && studentId.endsWith("_delete")) {
            studentId = studentId.substring(0, studentId.length() - "_delete".length());
        }
        return Map.of("publicId", user.getPublicId(), "studentId", studentId);
    }

    // 封禁账号：支持临时封禁和永久封禁，临时封禁要传天数，永久封禁的截止时间为空，到期后自动恢复。
    @PostMapping("/users/{publicId}/ban")
    public Map<String, Object> ban(Authentication authentication, @PathVariable String publicId, @RequestBody Map<String, Object> body) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        String type = String.valueOf(body.getOrDefault("type", "PERMANENT"));
        String reason = reasonOf(body);
        user.setStatus(UserStatus.BANNED);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setBanReason(reason);
        if (type.equals("TEMPORARY")) {
            int days = 7;
            try {
                days = Integer.parseInt(String.valueOf(body.getOrDefault("days", 7)));
            } catch (Exception ignored) {
            }
            user.setBanUntil(LocalDateTime.now().plusDays(days));
        } else {
            user.setBanUntil(null);
        }
        userRepository.save(user);
        adminAuditService.log(authentication.getName(), "BAN_USER", "USER", user.getId(), reason);
        return Map.of("ok", true);
    }

    // 解封账号：清除封禁状态和截止时间，并记审计日志。
    @PostMapping("/users/{publicId}/unban")
    public Map<String, Object> unban(Authentication authentication, @PathVariable String publicId, @RequestBody(required = false) Map<String, Object> body) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setBanUntil(null);
        user.setBanReason(null);
        userRepository.save(user);
        adminAuditService.log(authentication.getName(), "UNBAN_USER", "USER", user.getId(), reasonOf(body));
        return Map.of("ok", true);
    }

    // 举报列表：帖子举报和评论举报都在这里，举报人信息不公开，只展示目标和原因。
    @GetMapping("/reports")
    public org.springframework.data.domain.Page<Map<String, Object>> reports(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return paginate(reportRepository.findAll().stream().map(report -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", report.getId());
            item.put("targetType", report.getTargetType());
            item.put("reason", report.getReason());
            item.put("status", report.getStatus());
            item.put("postId", report.getPost() == null ? null : report.getPost().getId());
            item.put("postTitle", report.getPost() == null ? "" : report.getPost().getTitle());
            item.put("commentId", report.getComment() == null ? null : report.getComment().getId());
            item.put("commentContent", report.getComment() == null ? "" : report.getComment().getContent());
            return item;
        }).collect(Collectors.toList()), page, size);
    }

    // 标记举报已处理，并记审计日志。
    @PostMapping("/reports/{id}/handle")
    public Map<String, Object> handleReport(Authentication authentication, @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Report report = reportRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "report not found"));
        report.setStatus("HANDLED");
        reportRepository.save(report);
        adminAuditService.log(authentication.getName(), "HANDLE_REPORT", "REPORT", id, reasonOf(body));
        return Map.of("ok", true);
    }

    // 审计日志列表：按时间倒序，日志里只保存管理员公开 ID，不保存学号。
    @GetMapping("/audit-logs")
    public org.springframework.data.domain.Page<Map<String, Object>> auditLogs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return paginate(adminAuditLogRepository.findAllByOrderByCreatedAtDesc().stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("adminPublicId", log.getAdmin().getPublicId());
            item.put("action", log.getAction());
            item.put("targetType", log.getTargetType());
            item.put("targetId", log.getTargetId());
            item.put("reason", log.getReason());
            item.put("createdAt", log.getCreatedAt() == null ? null : log.getCreatedAt().toString());
            return item;
        }).collect(Collectors.toList()), page, size);
    }
}
