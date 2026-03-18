package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.lang.Nullable;

import lombok.Data;

/**
 * 更新用户请求DTO
 */
@Data
public class UpdateUserRequest {

    /**
     * 新密码（可选）
     */
    private String newPassword;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Nullable
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Nullable
    private String email;
}
