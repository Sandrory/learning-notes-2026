package com.example.demo.service.strategy.impl;

import com.example.demo.domain.entity.Product;
import com.example.demo.repository.ProductRepository;
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
 * 根据名称查询商品的策略实现
 */
@Component
public class ProductByNameStrategy implements QueryStrategy<Product> {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @SuppressWarnings("unchecked")
    public List<Product> executeQuery(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyList();
        }
        String name = params[0].toString();
        Optional<Product> product = productRepository.findByName(name);
        return product.map(Collections::singletonList).orElse(Collections.emptyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Product> executeQuery(Pageable pageable, Object... params) {
        if (params == null || params.length == 0) {
            return Page.empty(pageable);
        }
        String name = params[0].toString();
        List<Product> productList = productRepository.findByNameContainingIgnoreCase(name);
        return new PageImpl<>(productList, pageable, productList.size());
    }

    @Override
    public String getStrategyName() {
        return "productByName";
    }
}
