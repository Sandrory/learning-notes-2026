package com.example.demo.service.strategy.impl;

import com.example.demo.domain.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.strategy.QueryStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询可销售商品的策略实现
 * 状态为上架且库存大于0
 */
@Component
public class ProductsActiveForSaleStrategy implements QueryStrategy<Product> {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @SuppressWarnings("unchecked")
    public List<Product> executeQuery(Object... params) {
        return productRepository.findActiveForSale();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Product> executeQuery(Pageable pageable, Object... params) {
        return productRepository.findActiveForSale(pageable);
    }

    @Override
    public String getStrategyName() {
        return "productsActiveForSale";
    }
}
