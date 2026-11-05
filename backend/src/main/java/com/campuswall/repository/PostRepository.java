package com.campuswall.repository;

// 帖子数据访问：筛选查询和频率统计都在这里，频率统计用来限制短时间内批量发帖。
import com.campuswall.entity.CategoryCode;
import com.campuswall.entity.Post;
import com.campuswall.entity.PostStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("select p from Post p where p.status = :status and (:campusId is null or p.campus.id = :campusId) and (:category is null or p.category = :category) and (:keyword is null or p.title like concat('%', :keyword, '%') or p.content like concat('%', :keyword, '%')) order by p.createdAt desc")
    Page<Post> search(@Param("status") PostStatus status, @Param("campusId") Long campusId, @Param("category") CategoryCode category, @Param("keyword") String keyword, Pageable pageable);

    // 按多个校区筛选：订阅筛选就是用这个方法，校区列表为空时不做校区限制，由调用方决定是否返回空。
    @Query("select p from Post p where p.status = :status and p.campus.id in :campusIds and (:category is null or p.category = :category) and (:keyword is null or p.title like concat('%', :keyword, '%') or p.content like concat('%', :keyword, '%')) order by p.createdAt desc")
    Page<Post> searchByCampuses(@Param("status") PostStatus status, @Param("campusIds") java.util.List<Long> campusIds, @Param("category") CategoryCode category, @Param("keyword") String keyword, Pageable pageable);

    long countByAuthorIdAndCreatedAtAfter(Long authorId, LocalDateTime after);

    java.util.List<Post> findByAuthorId(Long authorId);

    long countByIpHashAndCreatedAtAfter(String ipHash, LocalDateTime after);
}
