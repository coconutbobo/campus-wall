package com.campuswall.service;

// 帖子核心服务：匿名、频率限制、敏感信息检测和本人管理都在这里。
// 匿名只是展示规则：公开接口里匿名帖子只返回匿名用户和空的作者公开 ID，不返回任何能把不同匿名帖子关联到同一个人的标识。
// 作者本人除外：本人登录后在自己的帖子列表和详情里能看到这是自己的帖子，也能编辑和删除，不会因为匿名就失去管理权。
// 频率限制用数据库计数实现：每人每小时最多发 10 条帖子、30 条评论，同一网络地址哈希每小时最多发 50 条帖子，IP 只是辅助，不单独作为封禁依据。
import com.campuswall.dto.PostCreateRequest;
import com.campuswall.entity.CategoryCode;
import com.campuswall.entity.Comment;
import com.campuswall.entity.Favorite;
import com.campuswall.entity.LostDetail;
import com.campuswall.entity.Post;
import com.campuswall.entity.PostImage;
import com.campuswall.entity.PostLike;
import com.campuswall.entity.PostStatus;
import com.campuswall.entity.QaDetail;
import com.campuswall.entity.SecondhandDetail;
import com.campuswall.entity.User;
import com.campuswall.entity.VentDetail;
import com.campuswall.repository.CampusRepository;
import com.campuswall.repository.CommentRepository;
import com.campuswall.repository.FavoriteRepository;
import com.campuswall.repository.LostDetailRepository;
import com.campuswall.repository.PostImageRepository;
import com.campuswall.repository.PostLikeRepository;
import com.campuswall.repository.PostRepository;
import com.campuswall.repository.QaDetailRepository;
import com.campuswall.repository.SecondhandDetailRepository;
import com.campuswall.repository.UserRepository;
import com.campuswall.repository.VentDetailRepository;
import com.campuswall.util.SensitiveInfoDetector;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CampusRepository campusRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final FavoriteRepository favoriteRepository;
    private final LostDetailRepository lostDetailRepository;
    private final SecondhandDetailRepository secondhandDetailRepository;
    private final QaDetailRepository qaDetailRepository;
    private final VentDetailRepository ventDetailRepository;
    private final com.campuswall.repository.UserCampusSubscriptionRepository subscriptionRepository;

    @Value("${app.limits.posts-per-hour:10}")
    private int postsPerHour;

    @Value("${app.limits.comments-per-hour:30}")
    private int commentsPerHour;

    @Value("${app.limits.posts-per-hour-per-ip:50}")
    private int postsPerHourPerIp;

    // 根据公开 ID 找到当前登录用户，找不到就当作未登录处理。
    public User currentUser(String publicId) {
        return userRepository.findByPublicId(publicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
    }

    // 把网络地址做一次哈希，只保留哈希值，不保存原始地址。
    public static String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // 判断帖子作者是不是当前登录用户，匿名帖子的本人判断也靠这个，不靠展示出来的名字。
    private boolean isMine(Post post, String currentPublicId) {
        return currentPublicId != null && post.getAuthor().getPublicId().equals(currentPublicId);
    }

    // 把帖子转成返回给前端的数据：匿名时作者公开 ID 为空，已注销作者显示为已注销用户，本人帖子会多一个是本人的标记。
    public Map<String, Object> toMap(Post post, boolean withComments, String currentPublicId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("campusId", post.getCampus().getId());
        map.put("campusName", post.getCampus().getName());
        map.put("category", post.getCategory().name());
        map.put("title", post.getTitle());
        map.put("content", post.getContent());
        map.put("anonymous", post.isAnonymous());
        boolean mine = isMine(post, currentPublicId);
        map.put("isMine", mine);
        if (post.isAnonymous()) {
            map.put("displayName", "匿名用户");
            map.put("authorPublicId", null);
        } else if (post.getAuthor().getStatus().name().equals("DELETED")) {
            map.put("displayName", "已注销用户");
            map.put("authorPublicId", null);
        } else {
            map.put("displayName", post.getAuthor().getNickname());
            map.put("authorPublicId", post.getAuthor().getPublicId());
        }
        map.put("contactInfo", post.getContactInfo());
        map.put("status", post.getStatus().name());
        map.put("createdAt", post.getCreatedAt() == null ? null : post.getCreatedAt().toString());
        map.put("likeCount", postLikeRepository.countByPostId(post.getId()));
        map.put("favoriteCount", favoriteRepository.countByPostId(post.getId()));
        map.put("images", postImageRepository.findByPostIdOrderBySortOrderAsc(post.getId()).stream().map(PostImage::getImageUrl).collect(Collectors.toList()));
        if (post.getCategory() == CategoryCode.LOST) {
            lostDetailRepository.findById(post.getId()).ifPresent(detail -> {
                map.put("lostKind", detail.getKind());
                map.put("location", detail.getLocation());
                map.put("happenedAt", detail.getHappenedAt() == null ? null : detail.getHappenedAt().toString());
            });
        }
        if (post.getCategory() == CategoryCode.SECONDHAND) {
            secondhandDetailRepository.findById(post.getId()).ifPresent(detail -> {
                map.put("price", detail.getPrice());
                map.put("conditionText", detail.getConditionText());
            });
        }
        if (post.getCategory() == CategoryCode.QA) {
            qaDetailRepository.findById(post.getId()).ifPresent(detail -> {
                map.put("solved", detail.isSolved());
                map.put("acceptedCommentId", detail.getAcceptedCommentId());
            });
        }
        if (post.getCategory() == CategoryCode.VENT) {
            ventDetailRepository.findById(post.getId()).ifPresent(detail -> map.put("mood", detail.getMood()));
        }
        if (withComments) {
            List<Map<String, Object>> comments = commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), "NORMAL").stream().map(comment -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", comment.getId());
                item.put("content", comment.getContent());
                item.put("anonymous", comment.isAnonymous());
                boolean commentMine = currentPublicId != null && comment.getAuthor().getPublicId().equals(currentPublicId);
                item.put("isMine", commentMine);
                if (comment.isAnonymous()) {
                    item.put("displayName", "匿名用户");
                    item.put("authorPublicId", null);
                } else if (comment.getAuthor().getStatus().name().equals("DELETED")) {
                    item.put("displayName", "已注销用户");
                    item.put("authorPublicId", null);
                } else {
                    item.put("displayName", comment.getAuthor().getNickname());
                    item.put("authorPublicId", comment.getAuthor().getPublicId());
                }
                item.put("createdAt", comment.getCreatedAt() == null ? null : comment.getCreatedAt().toString());
                return item;
            }).collect(Collectors.toList());
            map.put("comments", comments);
        }
        return map;
    }

    // 查询帖子列表：按校区、板块和关键词筛选，只返回正常状态的帖子。
    // 校区筛选有三种情况：指定单个校区就按单个校区查，指定多个校区就按多个校区查，订阅筛选就先查当前用户的订阅校区再按多个校区查。
    // 订阅筛选时如果用户一个校区都没订阅，就返回空页并让前端提示去订阅，而不是悄悄返回全部校区的内容。
    public Page<Map<String, Object>> list(Long campusId, java.util.List<Long> campusIds, boolean subscribedOnly, CategoryCode category, String keyword, int page, int size, String currentPublicId) {
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword;
        Page<Post> result;
        if (subscribedOnly) {
            User user = currentUser(currentPublicId);
            java.util.List<Long> subscribed = subscriptionRepository.findByUserId(user.getId()).stream().map(s -> s.getCampus().getId()).collect(Collectors.toList());
            if (subscribed.isEmpty()) {
                return Page.empty(PageRequest.of(page, size));
            }
            result = postRepository.searchByCampuses(PostStatus.NORMAL, subscribed, category, safeKeyword, PageRequest.of(page, size));
        } else if (campusIds != null && !campusIds.isEmpty()) {
            result = postRepository.searchByCampuses(PostStatus.NORMAL, campusIds, category, safeKeyword, PageRequest.of(page, size));
        } else {
            result = postRepository.search(PostStatus.NORMAL, campusId, category, safeKeyword, PageRequest.of(page, size));
        }
        return result.map(post -> toMap(post, false, currentPublicId));
    }

    // 查询帖子详情：包含图片、专属字段和评论，已删除的帖子当作不存在处理。
    public Map<String, Object> detail(Long id, String currentPublicId) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        if (post.getStatus() != PostStatus.NORMAL) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
        }
        return toMap(post, true, currentPublicId);
    }

    // 查询当前用户自己的帖子：包括匿名帖子，本人能看到这是自己的，也能从这里去编辑和删除。
    // 同样做分页，防止一个人发了很多帖子时一次返回全部。
    public Page<Map<String, Object>> myPosts(String publicId, int page, int size) {
        User user = currentUser(publicId);
        List<Map<String, Object>> all = postRepository.findByAuthorId(user.getId()).stream().map(post -> toMap(post, false, publicId)).collect(Collectors.toList());
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new org.springframework.data.domain.PageImpl<>(all.subList(from, to), PageRequest.of(safePage, safeSize), all.size());
    }

    // 发帖：先做频率限制和敏感信息检测，再保存帖子主体、专属字段和图片。
    @Transactional
    public Map<String, Object> create(String publicId, PostCreateRequest request, String ipHash) {
        User author = currentUser(publicId);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        if (postRepository.countByAuthorIdAndCreatedAtAfter(author.getId(), oneHourAgo) >= postsPerHour) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "posting too frequently");
        }
        if (ipHash != null && postRepository.countByIpHashAndCreatedAtAfter(ipHash, oneHourAgo) >= postsPerHourPerIp) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "posting too frequently from this network");
        }
        if (SensitiveInfoDetector.containsSensitiveInfo(request.title()) || SensitiveInfoDetector.containsSensitiveInfo(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content contains suspected phone number or ID card number, please remove it before posting");
        }
        Post post = new Post();
        post.setCampus(campusRepository.findById(request.campusId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "campus not found")));
        post.setAuthor(author);
        post.setCategory(request.category());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setAnonymous(request.anonymous());
        post.setContactInfo(request.contactInfo());
        post.setStatus(PostStatus.NORMAL);
        post.setIpHash(ipHash);
        postRepository.save(post);
        if (request.category() == CategoryCode.LOST) {
            LostDetail detail = new LostDetail();
            detail.setPost(post);
            detail.setKind(request.lostKind());
            detail.setLocation(request.location());
            detail.setHappenedAt(request.happenedAt());
            lostDetailRepository.save(detail);
        }
        if (request.category() == CategoryCode.SECONDHAND) {
            SecondhandDetail detail = new SecondhandDetail();
            detail.setPost(post);
            detail.setPrice(request.price());
            detail.setConditionText(request.conditionText());
            secondhandDetailRepository.save(detail);
        }
        if (request.category() == CategoryCode.QA) {
            QaDetail detail = new QaDetail();
            detail.setPost(post);
            detail.setSolved(false);
            qaDetailRepository.save(detail);
        }
        if (request.category() == CategoryCode.VENT) {
            VentDetail detail = new VentDetail();
            detail.setPost(post);
            detail.setMood(request.mood());
            ventDetailRepository.save(detail);
        }
        if (request.imageUrls() != null) {
            int order = 0;
            for (String url : request.imageUrls()) {
                PostImage image = new PostImage();
                image.setPost(post);
                image.setImageUrl(url);
                image.setSortOrder(order++);
                postImageRepository.save(image);
            }
        }
        return toMap(post, false, publicId);
    }

    // 作者编辑自己的帖子：匿名帖子同样靠作者身份判断，不靠展示名字，本人不会因为匿名就失去编辑权。
    @Transactional
    public Map<String, Object> editOwn(String publicId, Long id, String title, String content, String contactInfo) {
        User user = currentUser(publicId);
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not owner");
        }
        if (title != null && !title.isBlank()) {
            post.setTitle(title);
        }
        if (content != null && !content.isBlank()) {
            if (SensitiveInfoDetector.containsSensitiveInfo(content)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content contains suspected phone number or ID card number");
            }
            post.setContent(content);
        }
        if (contactInfo != null) {
            post.setContactInfo(contactInfo);
        }
        postRepository.save(post);
        return toMap(post, false, publicId);
    }

    // 作者删除自己的帖子：只改状态为已删除，不真正从数据库里删掉，方便管理端追溯。
    @Transactional
    public void deleteOwn(String publicId, Long id) {
        User user = currentUser(publicId);
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not owner");
        }
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    // 发表评论：同样做频率限制和敏感信息检测，匿名评论的规则和匿名帖子一致。
    @Transactional
    public Map<String, Object> addComment(String publicId, Long postId, String content, boolean anonymous) {
        User user = currentUser(publicId);
        if (commentRepository.countByAuthorIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusHours(1)) >= commentsPerHour) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "commenting too frequently");
        }
        if (SensitiveInfoDetector.containsSensitiveInfo(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content contains suspected phone number or ID card number");
        }
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setContent(content);
        comment.setAnonymous(anonymous);
        comment.setStatus("NORMAL");
        commentRepository.save(comment);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", comment.getId());
        item.put("content", comment.getContent());
        item.put("anonymous", comment.isAnonymous());
        item.put("displayName", comment.isAnonymous() ? "匿名用户" : user.getNickname());
        item.put("authorPublicId", comment.isAnonymous() ? null : user.getPublicId());
        return item;
    }

    // 点赞切换：已经点过赞就取消，没点过就新增，同一个用户对同一个帖子只能有一条点赞记录。
    @Transactional
    public boolean toggleLike(String publicId, Long postId) {
        User user = currentUser(publicId);
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        return postLikeRepository.findByPostIdAndUserId(postId, user.getId()).map(existing -> {
            postLikeRepository.delete(existing);
            return false;
        }).orElseGet(() -> {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(user);
            postLikeRepository.save(like);
            return true;
        });
    }

    // 收藏切换：逻辑和点赞一样，已经收藏就取消，没收藏就新增。
    @Transactional
    public boolean toggleFavorite(String publicId, Long postId) {
        User user = currentUser(publicId);
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        return favoriteRepository.findByPostIdAndUserId(postId, user.getId()).map(existing -> {
            favoriteRepository.delete(existing);
            return false;
        }).orElseGet(() -> {
            Favorite favorite = new Favorite();
            favorite.setPost(post);
            favorite.setUser(user);
            favoriteRepository.save(favorite);
            return true;
        });
    }
}
