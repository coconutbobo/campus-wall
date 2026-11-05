package com.campuswall.entity;

// 帖子实体：匿名只是一个展示开关，不是独立身份。
// 匿名帖子的作者字段在数据库里仍然保留，方便管理员处理恶意内容，但公开接口永远不会把作者的公开 ID、学号暴露给其他人。
// 发帖时的网络地址只存哈希，不存原始地址，IP 只作为辅助风控，绝不作为唯一的封禁依据，因为宿舍和教学楼经常共用出口。
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CategoryCode category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 是否匿名展示，true 时其他人只看到匿名用户，作者本人和管理端除外。
    @Column(nullable = false)
    private boolean anonymous = false;

    @Column(length = 128)
    private String contactInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostStatus status = PostStatus.NORMAL;

    // 发帖网络地址的哈希值，只用于频率统计和辅助风控，不存原始地址。
    @Column(length = 64)
    private String ipHash;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
