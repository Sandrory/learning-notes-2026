package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;
import com.example.demo.service.strategy.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * 用户管理控制器
 * 提供分页查询接口和权限控制
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 分页查询用户
     *
     * 权限控制（通过@PreAuthorize）：
     * - ADMIN角色：可以查询所有用户
     * - USER角色：只能查询自己的信息
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param strategy 查询策略（userById, userByUsername, usersByRole, allUsers）
     * @param params 查询参数
     * @return 分页用户数据
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<UserResponse>> findUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam String strategy,
            @RequestParam(required = false) String params) {

        // 创建分页参数
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 准备查询参数
        Object[] queryParams = null;
        if (params != null && !params.isEmpty()) {
            switch (strategy) {
                case UserConstants.USER_QUERY_BY_ID:
                    queryParams = new Object[]{Long.valueOf(params)};
                    break;
                case UserConstants.USER_QUERY_BY_USERNAME:
                case UserConstants.USER_QUERY_BY_EMAIL:
                case UserConstants.USER_QUERY_BY_PHONE:
                    queryParams = new Object[]{params};
                    break;
                case UserConstants.USER_QUERY_BY_ROLE:
                    queryParams = new Object[]{Role.fromCode(params)};
                    break;
                default:
                    queryParams = new Object[]{};
            }
        }

        // 执行查询（带权限控制）
        Page<User> userPage = userService.findUsers(pageable, strategy, queryParams);

        // 转换为DTO响应
        Page<UserResponse> responsePage = convertToUserResponsePage(userPage, pageable);

        return ResponseEntity.ok(responsePage);
    }

    /**
     * 根据ID查询单个用户
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/id")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserResponse> findUserById(@RequestParam Long id) {
        Pageable pageable = PageRequest.of(0, 1);
        Page<User> userPage = userService.findUsers(
            pageable,
            UserConstants.USER_QUERY_BY_ID,
            id
        );

        if (userPage.hasContent()) {
            User user = userPage.getContent().get(0);
            return ResponseEntity.ok(convertToUserResponse(user));
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 根据角色查询用户
     * 只有ADMIN可以查询其他角色
     */
    @GetMapping("/by-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> findUsersByRole(
            @RequestParam String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userService.findUsers(
            pageable,
            UserConstants.USER_QUERY_BY_ROLE,
            Role.fromCode(role)
        );

        Page<UserResponse> responsePage = convertToUserResponsePage(userPage, pageable);
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 查询当前登录用户信息
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();

        Pageable pageable = PageRequest.of(0, 1);
        Page<User> userPage = userService.findUsers(
            pageable,
            UserConstants.USER_QUERY_BY_USERNAME,
            username
        );

        if (userPage.hasContent()) {
            User user = userPage.getContent().get(0);
            return ResponseEntity.ok(convertToUserResponse(user));
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 转换User到UserResponse
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 转换Page<User>到Page<UserResponse>
     */
    private Page<UserResponse> convertToUserResponsePage(Page<User> userPage, Pageable pageable) {
        return new PageImpl<>(
                userPage.getContent().stream()
                        .map(this::convertToUserResponse)
                        .collect(Collectors.toList()),
                pageable,
                userPage.getTotalElements()
        );
    }
}
