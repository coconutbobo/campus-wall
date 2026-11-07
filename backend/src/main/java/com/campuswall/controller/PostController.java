package com.campuswall.controller;

// 帖子接口：公开的列表和详情，登录后的发帖、编辑和删除自己的帖子、评论、点赞、收藏和举报都在这里。
// 公开接口返回的作者信息只包含展示用的昵称或匿名用户，以及非匿名时的公开 ID，绝不包含学号和数据库主键。
// 发帖时取到的网络地址会先做哈希再交给服务层，原始地址不会保存到帖子记录里。
import com.campuswall.dto.PostCreateRequest;
import com.campuswall.entity.CategoryCode;
import com.campuswall.entity.Comment;
import com.campuswall.entity.Post;
import com.campuswall.entity.Report;
import com.campuswall.repository.CommentRepository;
import com.campuswall.repository.PostRepository;
import com.campuswall.repository.ReportRepository;
import com.campuswall.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final CommentRepository commentRepository;

    // 帖子列表：未登录也能看，登录用户能看到哪些是自己的帖子。
    // 每页条数做了上限，防止一次请求拉回全部帖子，默认 20 条，最多 50 条。
    @GetMapping
    public Page<Map<String, Object>> list(Authentication authentication, @RequestParam(required = false) Long campusId, @RequestParam(required = false) java.util.List<Long> campusIds, @RequestParam(defaultValue = "false") boolean subscribedOnly, @RequestParam(required = false) CategoryCode category, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        String current = authentication == null ? null : authentication.getName();
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        return postService.list(campusId, campusIds, subscribedOnly, category, keyword, safePage, safeSize, current);
    }

    // 帖子详情：包含图片、专属字段和正常状态的评论。
    @GetMapping("/{id}")
    public Map<String, Object> detail(Authentication authentication, @PathVariable Long id) {
        String current = authentication == null ? null : authentication.getName();
        return postService.detail(id, current);
    }

    // 发帖：把网络地址哈希后交给服务层，服务层再做频率限制。
    @PostMapping
    public Map<String, Object> create(Authentication authentication, HttpServletRequest request, @Valid @RequestBody PostCreateRequest body) {
        return postService.create(authentication.getName(), body, PostService.hashIp(request.getRemoteAddr()));
    }

    // 作者编辑自己的帖子：匿名帖子同样靠作者身份判断，本人不会因为匿名就失去编辑权。
    // 编辑页和发帖用同一套字段，整个 body 交给服务层，校区和板块不在里面改。
    @PatchMapping("/{id}")
    public Map<String, Object> editOwn(Authentication authentication, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return postService.editOwn(authentication.getName(), id, body);
    }

    // 作者删除自己的帖子。
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteOwn(Authentication authentication, @PathVariable Long id) {
        postService.deleteOwn(authentication.getName(), id);
        return Map.of("ok", true);
    }

    // 评论：评论也可以选择匿名，匿名规则和帖子一致。
    @PostMapping("/{id}/comments")
    public Map<String, Object> comment(Authentication authentication, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String content = String.valueOf(body.getOrDefault("content", ""));
        boolean anonymous = Boolean.parseBoolean(String.valueOf(body.getOrDefault("anonymous", false)));
        return postService.addComment(authentication.getName(), id, content, anonymous);
    }

    // 点赞切换。
    @PostMapping("/{id}/like")
    public Map<String, Object> like(Authentication authentication, @PathVariable Long id) {
        boolean liked = postService.toggleLike(authentication.getName(), id);
        return Map.of("liked", liked);
    }

    // 收藏切换。
    @PostMapping("/{id}/favorite")
    public Map<String, Object> favorite(Authentication authentication, @PathVariable Long id) {
        boolean favorited = postService.toggleFavorite(authentication.getName(), id);
        return Map.of("favorited", favorited);
    }

    // 举报帖子：举报原因会保存下来，等管理员处理，举报人信息不公开。
    @PostMapping("/{id}/report")
    public Map<String, Object> report(Authentication authentication, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        Report report = new Report();
        report.setReporter(postService.currentUser(authentication.getName()));
        report.setPost(post);
        report.setTargetType("POST");
        report.setReason(String.valueOf(body.getOrDefault("reason", "")));
        report.setStatus("PENDING");
        reportRepository.save(report);
        return Map.of("ok", true);
    }

    // 举报评论：举报时一并保存评论所属的帖子，方便管理员定位上下文。
    @PostMapping("/comments/{commentId}/report")
    public Map<String, Object> reportComment(Authentication authentication, @PathVariable Long commentId, @RequestBody Map<String, Object> body) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));
        Report report = new Report();
        report.setReporter(postService.currentUser(authentication.getName()));
        report.setPost(comment.getPost());
        report.setComment(comment);
        report.setTargetType("COMMENT");
        report.setReason(String.valueOf(body.getOrDefault("reason", "")));
        report.setStatus("PENDING");
        reportRepository.save(report);
        return Map.of("ok", true);
    }
}
