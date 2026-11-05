package com.campuswall.repository;

// 评论数据访问：公开详情只返回正常状态的评论，已删除的评论不会出现在公开接口里。
import com.campuswall.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, String status);

    long countByAuthorIdAndCreatedAtAfter(Long authorId, LocalDateTime after);

    java.util.List<Comment> findByAuthorId(Long authorId);
}
