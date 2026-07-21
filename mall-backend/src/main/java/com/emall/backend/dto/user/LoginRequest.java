package com.emall.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "账号不能为空")
        @Size(max = 50, message = "账号不能超过 50 个字符") String username,
        @NotBlank(message = "密码不能为空")
        @Size(max = 72, message = "密码不能超过 72 个字符") String password) {
}
