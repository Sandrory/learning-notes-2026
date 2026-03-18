package com.example.demo.service.strategy;

/**
 * 商品查询策略常量类
 * 定义了所有可用的商品查询策略名称
 */
public class ProductConstants {

    private ProductConstants() {
        // 私有构造函数，防止实例化
    }

    /**
     * 根据ID查询策略
     */
    public static final String PRODUCT_QUERY_BY_ID = "productById";

    /**
     * 根据名称查询策略
     */
    public static final String PRODUCT_QUERY_BY_NAME = "productByName";

    /**
     * 根据状态查询策略
     */
    public static final String PRODUCT_QUERY_BY_STATUS = "productsByStatus";

    /**
     * 根据名称模糊查询策略
     */
    public static final String PRODUCT_QUERY_BY_NAME_LIKE = "productsByNameLike";

    /**
     * 根据价格范围查询策略
     */
    public static final String PRODUCT_QUERY_BY_PRICE_RANGE = "productsByPriceRange";

    /**
     * 根据库存查询策略
     */
    public static final String PRODUCT_QUERY_BY_STOCK = "productsByStock";

    /**
     * 查询可销售的商品策略
     */
    public static final String PRODUCT_QUERY_ACTIVE_FOR_SALE = "productsActiveForSale";

    /**
     * 查询库存预警商品策略
     */
    public static final String PRODUCT_QUERY_NEED_REORDER = "productsNeedReorder";

    /**
     * 查询所有商品策略
     */
    public static final String PRODUCT_QUERY_ALL = "allProducts";
}
