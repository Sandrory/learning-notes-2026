package com.example.demo.service.strategy.impl;

import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.strategy.QueryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询所有用户的策略实现
 */
@Component
public class AllUsersStrategy implements QueryStrategy<User> {

    @Autowired
    private UserRepository userRepository;

    @Override
    @SuppressWarnings("unchecked")
    public List<User> executeQuery(Object... params) {
        return userRepository.findAll();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<User> executeQuery(Pageable pageable, Object... params) {
        return userRepository.findAll(pageable);
    }

    @Override
    public String getStrategyName() {
        return "allUsers";
    }
}