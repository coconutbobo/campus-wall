package com.campuswall.repository;

// 用户数据访问：登录和令牌校验都走公开 ID，不走学号，学号只在注册和登录核对时用一次。
import com.campuswall.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentId(String studentId);
    Optional<User> findByPublicId(String publicId);
    boolean existsByStudentId(String studentId);
}
