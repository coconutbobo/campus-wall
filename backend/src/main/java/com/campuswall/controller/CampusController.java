// 校园墙项目
// 校区接口：只提供校区列表，前端发帖和筛选时用它来展示校区选项。
package com.campuswall.controller;

import com.campuswall.entity.Campus;
import com.campuswall.repository.CampusRepository;
import java.util.List;
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
    // 查询帖子列表：按校区、板块和关键词筛选，只返回正常状态的帖子。
    public List<Campus> list() {
        return campusRepository.findAll();
    }
}
