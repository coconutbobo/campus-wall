// 校园墙项目
// 请求数据对象：前端传给后端的数据结构，校验规则写在字段上，不合格的请求会在进入服务前被拦截。
package com.campuswall.dto;

import com.campuswall.entity.CategoryCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

    // 方法：PostCreateRequest。
public record PostCreateRequest(
        @NotNull Long campusId,
        @NotNull CategoryCode category,
        @NotBlank String title,
        @NotBlank String content,
        boolean anonymous,
        boolean showContactInfo,
        List<String> imageUrls,
        String lostKind,
        String location,
        LocalDateTime happenedAt,
        BigDecimal price,
        String conditionText,
        String mood
) {
}
