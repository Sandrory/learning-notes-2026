package com.example.demo.dto;

import com.example.demo.domain.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应DTO
 */
@Data
@Builder
public class ProductResponse {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 状态
     */
    private ProductStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}