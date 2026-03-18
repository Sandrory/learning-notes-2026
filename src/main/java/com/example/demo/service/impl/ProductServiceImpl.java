package com.example.demo.service.impl;

import com.example.demo.domain.entity.Product;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ProductStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ProductService;
import com.example.demo.service.strategy.ProductConstants;
import com.example.demo.service.strategy.QueryStrategy;
import com.example.demo.service.strategy.factory.QueryStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 商品服务实现
 * 使用策略工厂实现查询
 * @CacheConfig: 配置缓存名称
 */
@Service
@CacheConfig(cacheNames = "productCache")
public class ProductServiceImpl implements ProductService {

    @Autowired
    private QueryStrategyFactory<Product> productQueryFactory;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 分页查询商品
     * @Cacheable: 缓存查询结果
     * 缓存Key包含分页和排序信息
     * unless = "#result == null || #result.getTotalElements() == 0": 不缓存空结果
     */
    @Override
    @Cacheable(
        value = "productListCache",
        key = "#strategyName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString().hashCode()",
        unless = "#result == null || #result.getTotalElements() == 0"
    )
    public Page<Product> findProducts(org.springframework.data.domain.Pageable pageable, String strategyName, Object... params) {
        // 所有认证用户都可以查询商品
        QueryStrategy<Product> strategy = productQueryFactory.createStrategy(strategyName);
        return strategy.executeQuery(pageable, params);
    }

    /**
     * 管理库存
     * @CacheEvict: 商品变更时清理缓存
     * 清理商品详情和商品列表缓存
     */
    @Override
    @CacheEvict(
        value = {"productListCache", "productCache"},
        allEntries = true,
        beforeInvocation = true
    )
    public void manageStock(Long productId, Integer quantity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getCurrentUserFromAuthentication(authentication);

        // 库存管理需要ADMIN权限
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPER_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("库存管理需要ADMIN权限");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        if (quantity < 0) {
            product.reduceStock(Math.abs(quantity));
        } else {
            product.increaseStock(quantity);
        }

        productRepository.save(product);
    }

    /**
     * 保存商品（演示用）
     * @CacheEvict: 保存时清理缓存
     */
    @CacheEvict(value = {"productListCache", "productCache"}, allEntries = true, beforeInvocation = true)
    public void saveProduct(Product product) {
        System.out.println("保存商品: " + product.getName());
        productRepository.save(product);
    }

    /**
     * 从认证信息获取当前用户
     * 实际项目中应该从数据库查询
     */
    private User getCurrentUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        Role role;

        if (username.startsWith("admin")) {
            role = Role.ADMIN;
        } else if (username.startsWith("superadmin")) {
            role = Role.SUPER_ADMIN;
        } else {
            role = Role.USER;
        }

        User user = new User(username, "password123", role);
        user.setEmail(username + "@example.com");
        user.setPhone("13800138000");
        return user;
    }
}
