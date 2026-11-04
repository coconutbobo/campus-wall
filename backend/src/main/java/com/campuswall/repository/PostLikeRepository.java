// 校园墙项目
// 数据访问接口：继承 JPA 接口，不用手写增删改查，复杂的帖子筛选在查询方法里定义。
package com.campuswall.repository;

import com.campuswall.entity.PostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
