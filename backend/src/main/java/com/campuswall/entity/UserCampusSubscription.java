package com.campuswall.entity;

// 校区订阅：用户可以订阅一个或多个校区，首页可以选择只看订阅校区的内容。
// 校区不是简单的分类标签，而是帖子的核心字段，每条帖子都必须有校区，订阅就是围绕这个字段做的用户偏好。
// 同一个用户对同一个校区只能订阅一次，数据库唯一约束保证不会重复。
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_campus_subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "campus_id"}))
@Getter
@Setter
@NoArgsConstructor
public class UserCampusSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
