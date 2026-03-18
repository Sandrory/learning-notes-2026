package com.example.demo.service;

import com.example.demo.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 商品服务接口
 * 包含分页查询和权限控制
 */
public interface ProductService {

    /**
     * 根据策略名称分页查询商品
     * 所有认证用户都可以查询商品
     *
     * @param pageable 分页参数
     * @param strategyName 查询策略名称
     * @param params 查询参数
     * @return 分页商品数据
     */
    Page<Product> findProducts(Pageable pageable, String strategyName, Object... params);

    /**
     * 库存管理方法
     * 需要ADMIN权限
     */
    void manageStock(Long productId, Integer quantity);
}
