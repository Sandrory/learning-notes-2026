package com.example.demo.dto;

import com.example.demo.domain.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应DTO
 */
@Data
@Builder
public class UserResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 角色
     */
    private Role role;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
