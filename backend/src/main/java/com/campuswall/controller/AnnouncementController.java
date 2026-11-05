package com.campuswall.controller;

// 公告接口：所有人都能看公告列表，只有管理员能发和删，权限在安全配置里按方法和角色拦。
import com.campuswall.entity.Announcement;
import com.campuswall.repository.AnnouncementRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementRepository announcementRepository;

    // 公告列表：不用登录也能看，最新在最前面。
    @GetMapping
    public List<Map<String, Object>> list() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("content", item.getContent());
            map.put("createdAt", item.getCreatedAt() == null ? null : item.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
    }

    // 发公告：只有管理员能调，安全配置会拦住普通账号。
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String title = body.get("title") == null ? "" : String.valueOf(body.get("title")).trim();
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content")).trim();
        if (title.isEmpty() || content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title and content are required");
        }
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcementRepository.save(announcement);
        return Map.of("id", announcement.getId(), "ok", true);
    }

    // 删公告：只有管理员能调。
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        announcementRepository.deleteById(id);
        return Map.of("ok", true);
    }
}
