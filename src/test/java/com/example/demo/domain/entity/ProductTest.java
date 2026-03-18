package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Product实体测试类
 * 演示充血模型的使用
 */
class ProductTest {

    @Test
    @DisplayName("测试创建商品成功")
    void testCreateProductSuccess() {
        // 当
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 那么
        assertNotNull(product);
        assertEquals("测试商品", product.getName());
        assertEquals(new BigDecimal("99.99"), product.getPrice());
        assertEquals(100, product.getStock());
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
        assertNotNull(product.getCreatedAt());
    }

    @Test
    @DisplayName("测试检查商品库存充足")
    void testHasEnoughStock() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当并那么
        assertTrue(product.hasEnoughStock(50));
        assertTrue(product.hasEnoughStock(100));
        assertFalse(product.hasEnoughStock(101));
        assertFalse(product.hasEnoughStock(0));
        assertFalse(product.hasEnoughStock(null));
    }

    @Test
    @DisplayName("测试扣减库存成功")
    void testReduceStockSuccess() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当
        Integer remainingStock = product.reduceStock(30);

        // 那么
        assertEquals(70, remainingStock);
        assertEquals(70, product.getStock());
    }

    @Test
    @DisplayName("测试扣减库存失败（库存不足）")
    void testReduceStockFail() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 10);

        // 当并那么
        assertThrows(IllegalStateException.class,
                () -> product.reduceStock(20));
    }

    @Test
    @DisplayName("测试扣减库存到0时自动更新状态为售罄")
    void testReduceStockToZero() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 50);

        // 当
        product.reduceStock(50);

        // 那么
        assertEquals(0, product.getStock());
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
    }

    @Test
    @DisplayName("测试增加库存成功")
    void testIncreaseStockSuccess() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当
        Integer newStock = product.increaseStock(50);

        // 那么
        assertEquals(150, newStock);
        assertEquals(150, product.getStock());
    }

    @Test
    @DisplayName("测试增加库存失败（无效数量）")
    void testIncreaseStockFail() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当并那么
        assertThrows(IllegalArgumentException.class,
                () -> product.increaseStock(0));
        assertThrows(IllegalArgumentException.class,
                () -> product.increaseStock(-10));
        assertThrows(IllegalArgumentException.class,
                () -> product.increaseStock(null));
    }

    @Test
    @DisplayName("测试从售罄状态增加库存后恢复为上架状态")
    void testIncreaseStockFromOutOfStock() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 0);
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());

        // 当
        product.increaseStock(10);

        // 那么
        assertEquals(10, product.getStock());
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
    }

    @Test
    @DisplayName("测试设置库存数量（覆盖当前库存）")
    void testSetStockQuantity() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当
        product.setStockQuantity(200);

        // 那么
        assertEquals(200, product.getStock());
    }

    @Test
    @DisplayName("测试设置库存为0时自动更新状态为售罄")
    void testSetStockQuantityToZero() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当
        product.setStockQuantity(0);

        // 那么
        assertEquals(0, product.getStock());
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
    }

    @Test
    @DisplayName("测试更新商品价格成功")
    void testUpdatePriceSuccess() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当
        product.updatePrice(new BigDecimal("149.99"));

        // 那么
        assertEquals(new BigDecimal("149.99"), product.getPrice());
    }

    @Test
    @DisplayName("测试更新商品价格失败（负数价格）")
    void testUpdatePriceFail() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当并那么
        assertThrows(IllegalArgumentException.class,
                () -> product.updatePrice(new BigDecimal("-10.00")));
        assertThrows(IllegalArgumentException.class,
                () -> product.updatePrice(null));
    }

    @Test
    @DisplayName("测试更新商品状态成功")
    void testUpdateStatusSuccess() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当
        product.updateStatus(ProductStatus.INACTIVE);

        // 那么
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }

    @Test
    @DisplayName("测试更新商品状态为已删除（库存为0）")
    void testUpdateStatusToDeleted() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);
        product.setStockQuantity(0);
        product.updateStatus(ProductStatus.OUT_OF_STOCK);

        // 当
        product.updateStatus(ProductStatus.DELETED);

        // 那么
        assertEquals(ProductStatus.DELETED, product.getStatus());
    }

    @Test
    @DisplayName("测试删除商品失败（库存大于0）")
    void testDeleteFail() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);

        // 当并那么
        assertThrows(IllegalStateException.class,
                () -> product.updateStatus(ProductStatus.DELETED));
        assertThrows(IllegalStateException.class, product::delete);
    }

    @Test
    @DisplayName("测试检查商品是否可销售")
    void testIsAvailableForSale() {
        // 假设
        Product availableProduct = new Product("测试商品1", new BigDecimal("99.99"), 100);
        Product outOfStockProduct = new Product("测试商品2", new BigDecimal("99.99"), 0);
        Product inactiveProduct = new Product("测试商品3", new BigDecimal("99.99"), 100);
        inactiveProduct.deactivate();

        // 那么
        assertTrue(availableProduct.isAvailableForSale());
        assertFalse(outOfStockProduct.isAvailableForSale());
        assertFalse(inactiveProduct.isAvailableForSale());
    }

    @Test
    @DisplayName("测试激活商品（上架）")
    void testActivate() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 100);
        product.deactivate();
        assertEquals(ProductStatus.INACTIVE, product.getStatus());

        // 当
        product.activate();

        // 那么
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
    }

    @Test
    @DisplayName("测试激活商品失败（库存为0）")
    void testActivateFail() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 0);
        product.deactivate();

        // 当并那么
        assertThrows(IllegalStateException.class, product::activate);
    }

    @Test
    @DisplayName("测试创建商品时验证名称")
    void testCreateProductNameValidation() {
        // 当并那么
        assertThrows(IllegalArgumentException.class,
                () -> new Product("", new BigDecimal("99.99"), 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Product(" ", new BigDecimal("99.99"), 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Product("a".repeat(201), new BigDecimal("99.99"), 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Product(null, new BigDecimal("99.99"), 100));
    }

    @Test
    @DisplayName("测试创建商品时验证价格")
    void testCreateProductPriceValidation() {
        // 当并那么
        assertThrows(IllegalArgumentException.class,
                () -> new Product("测试商品", null, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Product("测试商品", new BigDecimal("-10.00"), 100));
    }

    @Test
    @DisplayName("测试创建商品时验证库存")
    void testCreateProductStockValidation() {
        // 当并那么
        assertThrows(IllegalArgumentException.class,
                () -> new Product("测试商品", new BigDecimal("99.99"), null));
        assertThrows(IllegalArgumentException.class,
                () -> new Product("测试商品", new BigDecimal("99.99"), -10));
    }

    @Test
    @DisplayName("测试检查商品是否已删除")
    void testIsDeleted() {
        // 假设
        Product product = new Product("测试商品", new BigDecimal("99.99"), 0);
        product.setStockQuantity(0);
        product.delete();

        // 那么
        assertTrue(product.isDeleted());
    }
}
