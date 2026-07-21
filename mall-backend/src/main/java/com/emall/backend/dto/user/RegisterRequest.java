package com.emall.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 50, message = "账号长度必须为 3 到 50 个字符") String username,
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过 50 个字符") String nickname,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱不能超过 254 个字符") String email,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为 8 到 72 个字符") String password,
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "\\d{6}", message = "验证码必须为 6 位数字") String code) {
}
