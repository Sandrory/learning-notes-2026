package com.example.demo.service.strategy.impl;

import com.example.demo.domain.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.strategy.QueryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 根据ID查询商品的策略实现
 */
@Component
public class ProductByIdStrategy implements QueryStrategy<Product> {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @SuppressWarnings("unchecked")
    public List<Product> executeQuery(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyList();
        }
        Long id = castToLong(params[0]);
        Optional<Product> product = productRepository.findById(id);
        return product.map(Arrays::asList).orElse(Collections.emptyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Product> executeQuery(Pageable pageable, Object... params) {
        List<Product> list = executeQuery(params);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Override
    public String getStrategyName() {
        return "productById";
    }

    private Long castToLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        if (obj instanceof String) {
            return Long.valueOf((String) obj);
        }
        throw new IllegalArgumentException("参数类型不支持: " + obj.getClass().getName());
    }
}
