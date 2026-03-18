package com.example.demo.domain.enums;

/**
 * 商品状态枚举
 */
public enum ProductStatus {
    /**
     * 上架状态
     */
    ACTIVE("ACTIVE", "上架"),

    /**
     * 下架状态
     */
    INACTIVE("INACTIVE", "下架"),

    /**
     * 售罄状态
     */
    OUT_OF_STOCK("OUT_OF_STOCK", "售罄"),

    /**
     * 删除状态
     */
    DELETED("DELETED", "已删除");

    private final String code;
    private final String description;

    ProductStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取对应的状态
     * @param code 状态代码
     * @return 对应的ProductStatus，如果未找到返回null
     */
    public static ProductStatus fromCode(String code) {
        for (ProductStatus status : ProductStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断商品是否可销售
     * @return 如果商品处于上架状态返回true，否则返回false
     */
    public boolean isActiveForSale() {
        return this == ACTIVE;
    }
}
