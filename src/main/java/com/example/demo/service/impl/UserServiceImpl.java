package com.example.demo.service.impl;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.service.UserService;
import com.example.demo.service.strategy.QueryStrategy;
import com.example.demo.service.strategy.UserConstants;
import com.example.demo.service.strategy.factory.QueryStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 * 使用策略工厂实现查询，并添加权限控制
 * @CacheConfig: 配置缓存名称和Key生成策略
 */
@Service
@CacheConfig(cacheNames = "userCache")
public class UserServiceImpl implements UserService {

    @Autowired
    private QueryStrategyFactory<User> userQueryFactory;

    /**
     * 分页查询用户
     * @Cacheable: 查询结果会被缓存
     * 缓存Key格式：strategyName:page:size:sortBy:sortDirection:paramsHash
     * unless = "#result == null || #result.getTotalElements() == 0": 不缓存空结果页
     */
    @Override
    @Cacheable(
        value = "userListCache",
        key = "#strategyName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString().hashCode()",
        unless = "#result == null || #result.getTotalElements() == 0"
    )
    public Page<User> findUsers(Pageable pageable, String strategyName, Object... params) {

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getCurrentUserFromAuthentication(authentication);

        // 查询策略
        QueryStrategy<User> strategy = userQueryFactory.createStrategy(strategyName);

        // 执行查询
        Page<User> result = strategy.executeQuery(pageable, params);

        // 权限控制：USER只能查自己
        if (currentUser != null && currentUser.getRole() == Role.USER) {
            return filterUsersByPermission(result, currentUser);
        }

        // ADMIN可以看到所有用户
        return result;
    }

    /**
     * 从认证信息获取当前用户
     * 实际项目中应该从数据库查询认证信息对应的用户
     * 这里简化处理：根据角色模拟一个用户
     */
    private User getCurrentUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 从principal获取用户标识，实际项目应该查询数据库
        String username = authentication.getName();

        // 这里模拟：可以通过配置或数据库查询获取真实用户信息
        // 模拟：从用户名解析角色（真项目应该从数据库查询）
        Role role;
        if (username.startsWith("admin")) {
            role = Role.ADMIN;
        } else if (username.startsWith("superadmin")) {
            role = Role.SUPER_ADMIN;
        } else {
            role = Role.USER;
        }

        // 模拟返回一个用户（真实项目应该从数据库查询）
        User user = new User(username, "password123", role);
        user.setEmail(username + "@example.com");
        user.setPhone("13800138000");
        return user;
    }

    /**
     * 根据权限过滤用户数据
     * USER用户只能看到自己
     * @param users 查询结果
     * @param currentUser 当前用户
     * @return 过滤后的分页结果
     */
    private Page<User> filterUsersByPermission(Page<User> users, User currentUser) {
        // USER用户只能查看自己的信息
        if (users.hasContent()) {
            boolean hasCurrentUser = users.getContent().stream()
                .anyMatch(u -> u.getId().equals(currentUser.getId()));

            if (hasCurrentUser) {
                return new PageImpl<>(java.util.List.of(currentUser),
                    users.getPageable(), 1);
            }
        }

        // 如果查询结果中没有自己，返回空页
        return new PageImpl<>(java.util.Collections.emptyList(),
            users.getPageable(), 0);
    }

    /**
     * 保存用户（演示用）
     * @CacheEvict: 清理相关缓存
     * allEntries = true: 清理所有用户相关的缓存
     * beforeInvocation = true: 在方法执行前清理缓存（防止方法执行失败时缓存未清理）
     */
    @CacheEvict(value = "userListCache", allEntries = true, beforeInvocation = true)
    public void saveUser(User user) {
        // 这里应该是实际的数据库保存逻辑
        // 为了演示，我们使用QueryStrategy来模拟保存（实际项目应该使用Repository）
        System.out.println("保存用户: " + user.getUsername());

        // 清理单个用户缓存（如果存在）
        String userCacheKey = "cache:saas:userCache:userById:" + user.getId();
        // 注意：这里应该使用RedisTemplate删除，但由于是演示，暂不实现
    }
}
