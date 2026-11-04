// 校园墙项目
// 请求数据对象：前端传给后端的数据结构，校验规则写在字段上，不合格的请求会在进入服务前被拦截。
package com.campuswall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

    // 方法：RegisterRequest。
public record RegisterRequest(
        @NotBlank String studentId,
        @NotBlank @Size(max = 64) String nickname,
        @NotBlank @Size(min = 6, max = 64) String password,
        @NotNull Long campusId
) {
}
