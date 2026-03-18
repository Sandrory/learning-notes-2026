package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ProductStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 使用充血模型，封装领域行为
 */
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 商品名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 商品描述
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 商品价格
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 商品库存
     */
    @Column(name = "stock", nullable = false)
    private Integer stock;

    /**
     * 商品状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 空构造函数，JPA需要
     */
    protected Product() {
    }

    /**
     * 构造函数
     * @param name 商品名称
     * @param price 商品价格
     * @param stock 初始库存
     */
    public Product(String name, BigDecimal price, Integer stock) {
        this.name = validateName(name);
        this.price = validatePrice(price);
        this.stock = validateStock(stock);
        this.status = ProductStatus.ACTIVE;
        this.description = "";
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Getter方法 ====================

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ==================== Setter方法 ====================

    public void setName(String name) {
        this.name = validateName(name);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = validatePrice(price);
    }

    /**
     * 设置库存（仅内部使用，外部应使用领域行为方法）
     * @param stock 库存数量
     */
    private void setStock(Integer stock) {
        this.stock = stock;
    }

    // ==================== 私有验证方法 ====================

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("商品名称不能为空");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("商品名称长度不能超过200个字符");
        }
        return name.trim();
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("商品价格不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("商品价格不能为负数");
        }
        return price;
    }

    private Integer validateStock(Integer stock) {
        if (stock == null) {
            throw new IllegalArgumentException("商品库存不能为空");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("商品库存不能为负数");
        }
        return stock;
    }

    // ==================== 领域行为方法（充血模型） ====================

    /**
     * 检查商品是否有足够的库存
     * @param quantity 需要检查的数量
     * @return 如果库存充足返回true，否则返回false
     */
    public boolean hasEnoughStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        return this.stock >= quantity;
    }

    /**
     * 扣减库存
     * @param quantity 扣减数量
     * @return 返回剩余的库存数量
     * @throws IllegalStateException 如果库存不足
     */
    public Integer reduceStock(Integer quantity) {
        if (!hasEnoughStock(quantity)) {
            throw new IllegalStateException("商品库存不足，当前库存：" + stock);
        }
        this.stock = this.stock - quantity;

        // 如果库存为0，自动更新状态为售罄
        if (this.stock == 0) {
            updateStatus(ProductStatus.OUT_OF_STOCK);
        }

        return this.stock;
    }

    /**
     * 增加库存
     * @param quantity 增加数量
     * @return 返回增加后的库存数量
     */
    public Integer increaseStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("增加数量必须为正整数");
        }
        this.stock = this.stock + quantity;

        // 如果是从售罄状态更新库存，自动恢复为上架状态
        if (this.status == ProductStatus.OUT_OF_STOCK && this.stock > 0) {
            updateStatus(ProductStatus.ACTIVE);
        }

        return this.stock;
    }

    /**
     * 设置库存数量（覆盖当前库存）
     * @param newStock 新的库存数量
     */
    public void setStockQuantity(Integer newStock) {
        this.stock = validateStock(newStock);

        // 根据库存自动更新状态
        if (this.stock == 0) {
            updateStatus(ProductStatus.OUT_OF_STOCK);
        } else if (this.status == ProductStatus.OUT_OF_STOCK) {
            updateStatus(ProductStatus.ACTIVE);
        }
    }

    /**
     * 更新商品价格
     * @param newPrice 新价格
     */
    public void updatePrice(BigDecimal newPrice) {
        this.price = validatePrice(newPrice);
    }

    /**
     * 更新商品状态
     * @param newStatus 新状态
     */
    public void updateStatus(ProductStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("商品状态不能为空");
        }

        // 设置状态为已删除时，检查当前状态是否允许删除
        if (newStatus == ProductStatus.DELETED) {
            if (this.stock > 0) {
                throw new IllegalStateException("库存大于0的商品不能直接删除");
            }
        }

        this.status = newStatus;
    }

    /**
     * 检查商品是否可销售
     * @return 如果是上架状态返回true，否则返回false
     */
    public boolean isAvailableForSale() {
        return this.status == ProductStatus.ACTIVE && this.stock > 0;
    }

    /**
     * 激活商品（上架）
     * @throws IllegalStateException 如果库存为0，无法激活
     */
    public void activate() {
        if (this.stock == 0) {
            throw new IllegalStateException("库存为0的商品无法上架");
        }
        updateStatus(ProductStatus.ACTIVE);
    }

    /**
     * 禁用商品（下架）
     */
    public void deactivate() {
        updateStatus(ProductStatus.INACTIVE);
    }

    /**
     * 逻辑删除商品（设置状态为已删除）
     * @throws IllegalStateException 如果库存大于0，无法删除
     */
    public void delete() {
        updateStatus(ProductStatus.DELETED);
    }

    /**
     * 检查商品是否已被删除
     * @return 如果商品状态为已删除返回true，否则返回false
     */
    public boolean isDeleted() {
        return this.status == ProductStatus.DELETED;
    }

    // ==================== 重写Object方法 ====================

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
