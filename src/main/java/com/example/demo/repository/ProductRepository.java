package com.example.demo.repository;

import com.example.demo.domain.entity.Product;
import com.example.demo.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Product Repository接口
 * 继承JpaRepository，获得基本的CRUD功能
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 根据商品名称查找（精确匹配）
     * @param name 商品名称
     * @return Optional包装的商品对象
     */
    Optional<Product> findByName(String name);

    /**
     * 根据商品名称模糊查询
     * @param name 商品名称关键词
     * @return 商品列表
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * 根据商品状态查找
     * @param status 商品状态
     * @return 商品列表
     */
    List<Product> findByStatus(ProductStatus status);

    /**
     * 根据商品状态分页查询
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * 查找价格范围内的商品
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 商品列表
     */
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 查找价格范围内的商品（分页）
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 查找库存大于指定数量的商品
     * @param stock 库存数量
     * @return 商品列表
     */
    List<Product> findByStockGreaterThanEqual(Integer stock);

    /**
     * 查找库存大于指定数量的商品（分页）
     * @param stock 库存数量
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Product> findByStockGreaterThanEqual(Integer stock, Pageable pageable);

    /**
     * 查找库存小于指定数量的商品
     * @param stock 库存数量
     * @return 商品列表
     */
    List<Product> findByStockLessThan(Integer stock);

    /**
     * 查找可销售的商品（状态为上架且库存大于0）
     * @return 商品列表
     */
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.stock > 0")
    List<Product> findActiveForSale();

    /**
     * 查找可销售的商品（分页）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.stock > 0")
    Page<Product> findActiveForSale(Pageable pageable);

    /**
     * 根据商品名称和状态查找
     * @param name 商品名称关键词
     * @param status 商品状态
     * @return 商品列表
     */
    List<Product> findByNameContainingIgnoreCaseAndStatus(String name, ProductStatus status);

    /**
     * 根据商品名称和状态查找（分页）
     * @param name 商品名称关键词
     * @param status 商品状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, ProductStatus status, Pageable pageable);

    /**
     * 查找需要补货的商品（库存低于指定值）
     * @param threshold 库存阈值
     * @return 商品列表
     */
    @Query("SELECT p FROM Product p WHERE p.stock <= :threshold AND p.status = 'ACTIVE'")
    List<Product> findProductsNeedReorder(@Param("threshold") Integer threshold);

    /**
     * 查找创建时间范围内的商品
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 商品列表
     */
    List<Product> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 查找创建时间范围内的商品（分页）
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT p FROM Product p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    Page<Product> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    /**
     * 统计指定状态的商品数量
     * @param status 商品状态
     * @return 商品数量
     */
    long countByStatus(ProductStatus status);

    /**
     * 统计库存总量
     * @return 库存总量
     */
    @Query("SELECT SUM(p.stock) FROM Product p")
    Long sumStock();

    /**
     * 计算商品总销售额（库存 * 价格）
     * @return 总销售额
     */
    @Query("SELECT SUM(p.stock * p.price) FROM Product p")
    BigDecimal calculateTotalInventoryValue();

    /**
     * 查询指定价格以上商品
     * @param price 价格
     * @return 商品列表
     */
    List<Product> findByPriceGreaterThanEqual(BigDecimal price);

    /**
     * 查询指定价格以上商品（分页）
     * @param price 价格
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Product> findByPriceGreaterThanEqual(BigDecimal price, Pageable pageable);

    /**
     * 删除商品（批量）
     * @param ids 商品ID列表
     */
    @Modifying
    @Query("DELETE FROM Product p WHERE p.id IN :ids")
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据ID列表查找商品
     * @param ids 商品ID列表
     * @return 商品列表
     */
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findByIds(@Param("ids") List<Long> ids);
}
