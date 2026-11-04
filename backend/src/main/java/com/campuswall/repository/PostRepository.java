// 校园墙项目
// 数据访问接口：继承 JPA 接口，不用手写增删改查，复杂的帖子筛选在查询方法里定义。
package com.campuswall.repository;

import com.campuswall.entity.CategoryCode;
import com.campuswall.entity.Post;
import com.campuswall.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("select p from Post p where p.status = :status and (:campusId is null or p.campus.id = :campusId) and (:category is null or p.category = :category) and (:keyword is null or p.title like concat('%', :keyword, '%') or p.content like concat('%', :keyword, '%')) order by p.createdAt desc")
    Page<Post> search(@Param("status") PostStatus status, @Param("campusId") Long campusId, @Param("category") CategoryCode category, @Param("keyword") String keyword, Pageable pageable);
}
