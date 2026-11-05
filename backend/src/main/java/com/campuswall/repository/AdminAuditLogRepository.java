package com.campuswall.repository;

// 审计日志数据访问：按时间倒序查，方便管理员复核最近的操作。
import com.campuswall.entity.AdminAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();
}
