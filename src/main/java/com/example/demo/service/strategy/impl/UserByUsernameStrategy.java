package com.example.demo.service.strategy.impl;

import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.strategy.QueryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 根据用户名查询用户的策略实现
 */
@Component
public class UserByUsernameStrategy implements QueryStrategy<User> {

    @Autowired
    private UserRepository userRepository;

    @Override
    @SuppressWarnings("unchecked")
    public List<User> executeQuery(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyList();
        }
        String username = params[0].toString();
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(Collections::singletonList).orElse(Collections.emptyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<User> executeQuery(Pageable pageable, Object... params) {
        List<User> list = executeQuery(params);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Override
    public String getStrategyName() {
        return "userByUsername";
    }
}