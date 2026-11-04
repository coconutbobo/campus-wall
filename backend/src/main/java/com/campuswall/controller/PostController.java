// 校园墙项目
// 帖子接口：公开的列表和详情，登录后的发帖、删自己的帖子、评论、点赞、收藏和举报都在这里。
package com.campuswall.controller;

import com.campuswall.dto.PostCreateRequest;
import com.campuswall.entity.CategoryCode;
import com.campuswall.entity.Post;
import com.campuswall.entity.Report;
import com.campuswall.repository.PostRepository;
import com.campuswall.repository.ReportRepository;
import com.campuswall.service.PostService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;

    @GetMapping
    // 查询帖子列表：按校区、板块和关键词筛选，只返回正常状态的帖子。
    public Page<Map<String, Object>> list(@RequestParam(required = false) Long campusId, @RequestParam(required = false) CategoryCode category, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return postService.list(campusId, category, keyword, page, size);
    }

    @GetMapping("/{id}")
    // 查询帖子详情：包含图片、专属字段和评论，已删除的帖子当作不存在处理。
    public Map<String, Object> detail(@PathVariable Long id) {
        return postService.detail(id);
    }

    @PostMapping
    // 发帖：先找到作者和校区，保存帖子主体，再按板块保存对应的专属字段和图片。
    public Map<String, Object> create(Authentication authentication, @Valid @RequestBody PostCreateRequest request) {
        return postService.create(authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    // 作者删除自己的帖子：只改状态为已删除，不真正从数据库里删掉，方便管理端追溯。
    public Map<String, Object> deleteOwn(Authentication authentication, @PathVariable Long id) {
        postService.deleteOwn(authentication.getName(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/comments")
    // 评论接口：接收前端传来的评论内容和是否匿名。
    public Map<String, Object> comment(Authentication authentication, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String content = String.valueOf(body.getOrDefault("content", ""));
        boolean anonymous = Boolean.parseBoolean(String.valueOf(body.getOrDefault("anonymous", false)));
        return postService.addComment(authentication.getName(), id, content, anonymous);
    }

    @PostMapping("/{id}/like")
    // 点赞接口：调用点赞切换逻辑并返回当前是否已点赞。
    public Map<String, Object> like(Authentication authentication, @PathVariable Long id) {
        boolean liked = postService.toggleLike(authentication.getName(), id);
        return Map.of("liked", liked);
    }

    @PostMapping("/{id}/favorite")
    // 收藏接口：调用收藏切换逻辑并返回当前是否已收藏。
    public Map<String, Object> favorite(Authentication authentication, @PathVariable Long id) {
        boolean favorited = postService.toggleFavorite(authentication.getName(), id);
        return Map.of("favorited", favorited);
    }

    @PostMapping("/{id}/report")
    // 举报接口：把举报人和被举报帖子、举报原因保存下来，等管理员处理。
    public Map<String, Object> report(Authentication authentication, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        Report report = new Report();
        report.setReporter(postService.currentUser(authentication.getName()));
        report.setPost(post);
        report.setReason(String.valueOf(body.getOrDefault("reason", "")));
        report.setStatus("PENDING");
        reportRepository.save(report);
        return Map.of("ok", true);
    }
}
