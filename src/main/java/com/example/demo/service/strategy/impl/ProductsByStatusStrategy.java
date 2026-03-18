package com.example.demo.service.strategy.impl;

import com.example.demo.domain.entity.Product;
import com.example.demo.domain.enums.ProductStatus;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.strategy.QueryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 根据状态查询商品的策略实现
 */
@Component
public class ProductsByStatusStrategy implements QueryStrategy<Product> {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @SuppressWarnings("unchecked")
    public List<Product> executeQuery(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyList();
        }
        ProductStatus status = castToStatus(params[0]);
        return productRepository.findByStatus(status);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Product> executeQuery(Pageable pageable, Object... params) {
        if (params == null || params.length == 0) {
            return Page.empty(pageable);
        }
        ProductStatus status = castToStatus(params[0]);
        return productRepository.findByStatus(status, pageable);
    }

    @Override
    public String getStrategyName() {
        return "productsByStatus";
    }

    private ProductStatus castToStatus(Object obj) {
        if (obj instanceof ProductStatus) {
            return (ProductStatus) obj;
        }
        if (obj instanceof String) {
            return ProductStatus.fromCode((String) obj);
        }
        throw new IllegalArgumentException("参数类型不支持: " + obj.getClass().getName());
    }
}
