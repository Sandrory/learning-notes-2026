package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新商品请求DTO
 */
@Data
public class UpdateProductRequest {

    /**
     * 商品名称
     */
    @Size(max = 200, message = "商品名称长度不能超过200个字符")
    @Nullable
    private String name;

    /**
     * 商品描述
     */
    @Size(max = 1000, message = "商品描述长度不能超过1000个字符")
    @Nullable
    private String description;

    /**
     * 价格
     */
    @DecimalMin(value = "0.00", message = "商品价格不能为负数")
    @Nullable
    private BigDecimal price;

    /**
     * 库存
     */
    @Nullable
    private Integer stock;
}