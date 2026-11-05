package com.campuswall.service;

// 审计服务：管理员的每一次敏感操作都要写日志，日志里保存管理员的公开 ID、操作类型、目标和理由。
// 审计日志本身不保存学号，避免日志变成新的学号泄露渠道。
import com.campuswall.entity.AdminAuditLog;
import com.campuswall.entity.User;
import com.campuswall.repository.AdminAuditLogRepository;
import com.campuswall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final UserRepository userRepository;

    // 记录一次管理员操作，调用方必须传理由，理由为空时用默认文案补齐，保证每条日志都有据可查。
    public void log(String adminPublicId, String action, String targetType, Long targetId, String reason) {
        User admin = userRepository.findByPublicId(adminPublicId).orElse(null);
        if (admin == null) {
            return;
        }
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(admin);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setReason(reason == null || reason.isBlank() ? "未填写理由" : reason);
        adminAuditLogRepository.save(log);
    }
}
