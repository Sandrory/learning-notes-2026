package com.example.demo.service;

import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 用户服务接口
 * 包含分页查询和权限控制
 */
public interface UserService {

    /**
     * 根据策略名称分页查询用户
     *
     * 权限控制：
     * - ADMIN：可以查询所有用户
     * - USER：只能查询自己的信息
     *
     * @param pageable 分页参数
     * @param strategyName 查询策略名称
     * @param params 查询参数
     * @return 分页用户数据
     */
    Page<User> findUsers(Pageable pageable, String strategyName, Object... params);
}
