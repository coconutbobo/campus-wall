package com.campuswall.controller;

// 本人接口：当前用户自己的帖子列表，包括匿名帖子，本人能从这里管理自己的匿名帖子。
// 这个接口只返回本人的帖子，不会把别人的匿名帖子暴露出来。
// 校区订阅也在这个控制器里：用户可以订阅和取消订阅校区，首页可以选择只看订阅校区的内容。
import com.campuswall.entity.Campus;
import com.campuswall.entity.User;
import com.campuswall.entity.UserCampusSubscription;
import com.campuswall.repository.CampusRepository;
import com.campuswall.repository.UserCampusSubscriptionRepository;
import com.campuswall.repository.UserRepository;
import com.campuswall.service.PostService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {
    private final PostService postService;
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final UserCampusSubscriptionRepository subscriptionRepository;

    @GetMapping("/posts")
    public org.springframework.data.domain.Page<Map<String, Object>> myPosts(Authentication authentication, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return postService.myPosts(authentication.getName(), page, size);
    }

    // 当前用户的校区订阅列表：只返回校区主键、名称和编码。
    @GetMapping("/subscriptions")
    public List<Map<String, Object>> subscriptions(Authentication authentication) {
        User user = userRepository.findByPublicId(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        return subscriptionRepository.findByUserId(user.getId()).stream().map(subscription -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("campusId", subscription.getCampus().getId());
            item.put("campusName", subscription.getCampus().getName());
            item.put("campusCode", subscription.getCampus().getCode());
            return item;
        }).collect(Collectors.toList());
    }

    // 订阅一个校区：已经订阅过就直接返回成功，不会重复创建。
    @PostMapping("/subscriptions")
    public Map<String, Object> subscribe(Authentication authentication, @RequestBody Map<String, Object> body) {
        User user = userRepository.findByPublicId(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        Long campusId = Long.parseLong(String.valueOf(body.get("campusId")));
        Campus campus = campusRepository.findById(campusId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "campus not found"));
        if (subscriptionRepository.findByUserIdAndCampusId(user.getId(), campusId).isEmpty()) {
            UserCampusSubscription subscription = new UserCampusSubscription();
            subscription.setUser(user);
            subscription.setCampus(campus);
            subscriptionRepository.save(subscription);
        }
        return Map.of("ok", true);
    }

    // 取消订阅一个校区：没有订阅过也直接返回成功。
    @DeleteMapping("/subscriptions/{campusId}")
    public Map<String, Object> unsubscribe(Authentication authentication, @PathVariable Long campusId) {
        User user = userRepository.findByPublicId(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        subscriptionRepository.findByUserIdAndCampusId(user.getId(), campusId).ifPresent(subscriptionRepository::delete);
        return Map.of("ok", true);
    }
}
