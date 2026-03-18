package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建商品请求DTO
 */
@Data
public class CreateProductRequest {

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200个字符")
    private String name;

    /**
     * 商品描述
     */
    @Size(max = 1000, message = "商品描述长度不能超过1000个字符")
    private String description;

    /**
     * 价格
     */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.00", message = "商品价格不能为负数")
    private BigDecimal price;

    /**
     * 初始库存
     */
    @NotNull(message = "商品库存不能为空")
    private Integer stock;
}
