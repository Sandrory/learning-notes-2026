package com.example.demo.service.strategy.impl;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.strategy.QueryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 根据角色查询用户的策略实现
 */
@Component
public class UsersByRoleStrategy implements QueryStrategy<User> {

    @Autowired
    private UserRepository userRepository;

    @Override
    @SuppressWarnings("unchecked")
    public List<User> executeQuery(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyList();
        }
        Role role = castToRole(params[0]);
        return userRepository.findByRole(role);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<User> executeQuery(Pageable pageable, Object... params) {
        if (params == null || params.length == 0) {
            return Page.empty(pageable);
        }
        Role role = castToRole(params[0]);
        return userRepository.findByRole(role, pageable);
    }

    @Override
    public String getStrategyName() {
        return "usersByRole";
    }

    private Role castToRole(Object obj) {
        if (obj instanceof Role) {
            return (Role) obj;
        }
        if (obj instanceof String) {
            return Role.fromCode((String) obj);
        }
        throw new IllegalArgumentException("参数类型不支持: " + obj.getClass().getName());
    }
}