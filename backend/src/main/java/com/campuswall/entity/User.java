package com.campuswall.entity;

// 用户实体：内部用数据库主键做关联，对外只用公开 ID，学号只用来登录，绝不作为公开身份。
// 公开 ID 是随机生成的，与学号没有任何推导关系，别人拿到公开 ID 也推不出学号。
// 封禁分临时和永久：临时封禁要填截止时间，到期后自动恢复；永久封禁的截止时间为空。
// 注销后账号状态改为已注销，学号会被替换成不可恢复的占位值，密码会被清空。
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 对外公开的用户标识，随机生成，不从学号推导，公开接口只返回这个，不返回学号和数据库主键。
    @Column(nullable = false, unique = true, length = 64)
    private String publicId;

    // 令牌版本号：注销、封禁或改密码后递增，旧令牌里的版本号对不上就会被过滤器直接拒绝，解决令牌无法注销的问题。
    @Column(nullable = false)
    private int tokenVersion = 0;

    // 仅用于登录的学号，第一版不做学校系统核验，所以它只是登录账号，不是已认证的身份证明。
    // 学号绝不出现在公开接口里，管理员列表里也不出现，只有单独的、带理由和审计日志的接口才能查看。
    @Column(nullable = false, unique = true, length = 32)
    private String studentId;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(nullable = false)
    private String passwordHash;

    // 密保问题：注册时从预设列表里选一个，找回密码时要先按学号查出这个问题，再核对答案。
    // 只存问题本身，不存答案明文，答案用 BCrypt 加密后存在下面这个字段里。
    @Column(length = 128)
    private String securityQuestion;

    // 密保答案的 BCrypt 加密值，和密码一样只存加密结果，数据库里永远看不到答案原文。
    // 老账号（比如注册时还没有密保的 owner 账号）这里是空的，忘密码时要提示联系管理员。
    private String securityAnswerHash;

    // 上次改昵称的时间：注册不算改，所以新账号这里是空的，第一次改昵称不受 30 天限制；
    // 改过一次之后，下次要等满 30 天才能再改，后端按这个时间算下次可改日期。
    private LocalDateTime nicknameChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    // 临时封禁的截止时间，永久封禁时为空，到期后登录和令牌校验会把它当作已解封处理。
    private LocalDateTime banUntil;

    @Column(length = 500)
    private String banReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    private String avatarUrl;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (publicId == null || publicId.isBlank()) {
            publicId = UUID.randomUUID().toString();
        }
    }

    // 判断当前是否仍在封禁期内，临时封禁到期后返回 false，调用方要把它当作正常账号处理。
    public boolean isCurrentlyBanned() {
        if (status != UserStatus.BANNED) {
            return false;
        }
        if (banUntil == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(banUntil);
    }
}
