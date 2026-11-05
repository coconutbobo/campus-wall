// 校园墙项目
// 公告数据访问接口：按发布时间倒序查，最新公告排在最前面。
package com.campuswall.repository;

import com.campuswall.entity.Announcement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByCreatedAtDesc();
}
