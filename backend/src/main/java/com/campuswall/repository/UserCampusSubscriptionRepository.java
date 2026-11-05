package com.campuswall.repository;

// 校区订阅数据访问：按用户查订阅列表，按用户和校区查单条订阅，方便订阅和取消订阅时判断是否已存在。
import com.campuswall.entity.UserCampusSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCampusSubscriptionRepository extends JpaRepository<UserCampusSubscription, Long> {
    List<UserCampusSubscription> findByUserId(Long userId);

    Optional<UserCampusSubscription> findByUserIdAndCampusId(Long userId, Long campusId);
}
