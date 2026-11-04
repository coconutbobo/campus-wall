// 校园墙项目
// 请求数据对象：前端传给后端的数据结构，校验规则写在字段上，不合格的请求会在进入服务前被拦截。
package com.campuswall.dto;

import jakarta.validation.constraints.NotBlank;

    // 方法：LoginRequest。
public record LoginRequest(
        @NotBlank String studentId,
        @NotBlank String password
) {
}
