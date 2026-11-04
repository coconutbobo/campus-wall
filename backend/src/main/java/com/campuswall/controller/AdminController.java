// 校园墙项目
// 管理后台接口：只有管理员能调用，可以删帖、封禁和解封账号、处理举报，返回给管理端的数据已经去掉密码哈希。
package com.campuswall.controller;

import com.campuswall.entity.Post;
import com.campuswall.entity.PostStatus;
import com.campuswall.entity.Report;
import com.campuswall.entity.User;
import com.campuswall.entity.UserStatus;
import com.campuswall.repository.PostRepository;
import com.campuswall.repository.ReportRepository;
import com.campuswall.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @GetMapping("/posts")
    // 管理端帖子列表：返回给管理员看的帖子信息，已经去掉密码等敏感字段。
    public List<Map<String, Object>> posts() {
        return postRepository.findAll().stream().map(post -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", post.getId());
            item.put("title", post.getTitle());
            item.put("category", post.getCategory().name());
            item.put("status", post.getStatus().name());
            item.put("anonymous", post.isAnonymous());
            item.put("displayName", post.isAnonymous() ? "匿名用户" : post.getAuthor().getNickname());
            item.put("authorId", post.getAuthor().getId());
            item.put("createdAt", post.getCreatedAt() == null ? null : post.getCreatedAt().toString());
            return item;
        }).collect(Collectors.toList());
    }

    @DeleteMapping("/posts/{id}")
    // 管理员删帖：只改状态为已删除，不真正删除数据。
    public Map<String, Object> deletePost(@PathVariable Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
        return Map.of("ok", true);
    }

    @GetMapping("/users")
    // 管理端用户列表：返回给管理员看的用户信息，已经去掉密码哈希。
    public List<Map<String, Object>> users() {
        return userRepository.findAll().stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("studentId", user.getStudentId());
            item.put("nickname", user.getNickname());
            item.put("role", user.getRole().name());
            item.put("status", user.getStatus().name());
            item.put("campusName", user.getCampus() == null ? "" : user.getCampus().getName());
            return item;
        }).collect(Collectors.toList());
    }

    @PostMapping("/users/{id}/ban")
    // 封禁账号：被封禁的账号登录时会被拒绝，令牌过滤器也会拦截它的请求。
    public Map<String, Object> ban(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
        return Map.of("ok", true);
    }

    @PostMapping("/users/{id}/unban")
    // 解封账号：把账号状态恢复为正常，之后可以重新登录和使用。
    public Map<String, Object> unban(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return Map.of("ok", true);
    }

    @GetMapping("/reports")
    // 管理端举报列表：展示被举报的帖子和举报原因。
    public List<Map<String, Object>> reports() {
        return reportRepository.findAll().stream().map(report -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", report.getId());
            item.put("reason", report.getReason());
            item.put("status", report.getStatus());
            item.put("postId", report.getPost().getId());
            item.put("postTitle", report.getPost().getTitle());
            item.put("reporterNickname", report.getReporter().getNickname());
            return item;
        }).collect(Collectors.toList());
    }

    @PostMapping("/reports/{id}/handle")
    // 标记举报已处理：管理员处理完举报后把状态改为已处理。
    public Map<String, Object> handleReport(@PathVariable Long id) {
        Report report = reportRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "report not found"));
        report.setStatus("HANDLED");
        reportRepository.save(report);
        return Map.of("ok", true);
    }
}
