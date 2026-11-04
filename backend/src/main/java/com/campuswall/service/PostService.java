// 校园墙项目
// 帖子核心服务：帖子列表和详情的组装、发帖时按板块保存专属字段、评论、点赞和收藏的切换，匿名帖子在这里统一显示为匿名用户。
package com.campuswall.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

    // 根据令牌里的学号找到当前登录用户，找不到就当作未登录处理。
    public User currentUser(String studentId) {
        return userRepository.findByStudentId(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
    }

    // 把帖子转成返回给前端的数据：匿名帖子只显示匿名用户，不暴露学号和真实昵称。
    public Map<String, Object> toMap(Post post, boolean withComments) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("campusId", post.getCampus().getId());
        map.put("campusName", post.getCampus().getName());
        map.put("category", post.getCategory().name());
        map.put("title", post.getTitle());
        map.put("content", post.getContent());
        map.put("anonymous", post.isAnonymous());
        map.put("displayName", post.isAnonymous() ? "匿名用户" : post.getAuthor().getNickname());
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
            List<Map<String, Object>> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).stream().map(comment -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", comment.getId());
                item.put("content", comment.getContent());
                item.put("anonymous", comment.isAnonymous());
                item.put("displayName", comment.isAnonymous() ? "匿名用户" : comment.getAuthor().getNickname());
                item.put("createdAt", comment.getCreatedAt() == null ? null : comment.getCreatedAt().toString());
                return item;
            }).collect(Collectors.toList());
            map.put("comments", comments);
        }
        return map;
    }

    // 查询帖子列表：按校区、板块和关键词筛选，只返回正常状态的帖子。
    public Page<Map<String, Object>> list(Long campusId, CategoryCode category, String keyword, int page, int size) {
        Page<Post> result = postRepository.search(PostStatus.NORMAL, campusId, category, keyword == null || keyword.isBlank() ? null : keyword, PageRequest.of(page, size));
        return result.map(post -> toMap(post, false));
    }

    // 查询帖子详情：包含图片、专属字段和评论，已删除的帖子当作不存在处理。
    public Map<String, Object> detail(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        if (post.getStatus() != PostStatus.NORMAL) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
        }
        return toMap(post, true);
    }

    @Transactional
    // 发帖：先找到作者和校区，保存帖子主体，再按板块保存对应的专属字段和图片。
    public Map<String, Object> create(String studentId, PostCreateRequest request) {
        User author = currentUser(studentId);
        Post post = new Post();
        post.setCampus(campusRepository.findById(request.campusId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "campus not found")));
        post.setAuthor(author);
        post.setCategory(request.category());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setAnonymous(request.anonymous());
        post.setContactInfo(request.contactInfo());
        post.setStatus(PostStatus.NORMAL);
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
        return toMap(post, false);
    }

    @Transactional
    // 作者删除自己的帖子：只改状态为已删除，不真正从数据库里删掉，方便管理端追溯。
    public void deleteOwn(String studentId, Long id) {
        User user = currentUser(studentId);
        Post post = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not owner");
        }
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    @Transactional
    // 发表评论：评论也可以选择匿名，匿名评论同样只显示匿名用户。
    public Map<String, Object> addComment(String studentId, Long postId, String content, boolean anonymous) {
        User user = currentUser(studentId);
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setContent(content);
        comment.setAnonymous(anonymous);
        commentRepository.save(comment);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", comment.getId());
        item.put("content", comment.getContent());
        item.put("anonymous", comment.isAnonymous());
        item.put("displayName", comment.isAnonymous() ? "匿名用户" : user.getNickname());
        return item;
    }

    @Transactional
    // 点赞切换：已经点过赞就取消，没点过就新增，同一个用户对同一个帖子只能有一条点赞记录。
    public boolean toggleLike(String studentId, Long postId) {
        User user = currentUser(studentId);
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

    @Transactional
    // 收藏切换：逻辑和点赞一样，已经收藏就取消，没收藏就新增。
    public boolean toggleFavorite(String studentId, Long postId) {
        User user = currentUser(studentId);
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
