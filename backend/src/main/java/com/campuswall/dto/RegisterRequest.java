// 校园墙项目
// 请求数据对象：前端传给后端的数据结构，校验规则写在字段上，不合格的请求会在进入服务前被拦截。
package com.campuswall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

    // 方法：RegisterRequest。
public record RegisterRequest(
        // 学号只能是 11 位数字，这是注册时的硬性格式，登录时不做这个限制，老账号（比如 owner）还能正常登录。
        @NotBlank @Pattern(regexp = "\\d{11}", message = "studentId must be 11 digits") String studentId,
        @NotBlank @Size(max = 64) String nickname,
        @NotBlank @Size(min = 6, max = 64) String password,
        @NotNull Long campusId,
        // 密保问题必须从后端给出的预设列表里选，答案必填，找回密码时要靠它核对身份。
        @NotBlank String securityQuestion,
        @NotBlank @Size(max = 64) String securityAnswer
) {
}
