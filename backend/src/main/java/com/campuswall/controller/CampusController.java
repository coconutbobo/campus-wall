package com.campuswall.controller;

// 校区接口：只提供校区列表，返回的是校区主键、名称和编码的普通数据，不直接返回数据库实体。
// 直接返回实体在字段变多之后很容易把不该暴露的字段带出去，所以这里统一转成只含必要字段的数据。
import com.campuswall.repository.CampusRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campuses")
@RequiredArgsConstructor
public class CampusController {
    private final CampusRepository campusRepository;

    @GetMapping
    public List<Map<String, Object>> list() {
        return campusRepository.findAll().stream().map(campus -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", campus.getId());
            item.put("name", campus.getName());
            item.put("code", campus.getCode());
            return item;
        }).collect(Collectors.toList());
    }
}
