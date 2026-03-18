package com.example.demo.cache;

import com.example.demo.domain.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.service.ProductService;
import com.example.demo.service.strategy.UserConstants;
import com.example.demo.service.strategy.ProductConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis缓存性能测试类
 * 验证缓存命中率以及性能提升效果
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
@DisplayName("Redis缓存性能测试")
class RedisCachePerformanceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("测试User查询缓存性能 - 第一次无缓存，第二次命中缓存")
    void testUserCachePerformance() {
        // 创建分页参数
        Pageable pageable = PageRequest.of(
            0,
            10,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 使用StopWatch记录时间
        StopWatch stopWatch = new StopWatch();

        // ============ 第一次查询（无缓存） ============
        stopWatch.start("第一次查询 - 无缓存");
        var firstResult = userService.findUsers(pageable, UserConstants.USER_QUERY_ALL);
        stopWatch.stop();

        // 验证第一次查询结果不为空
        assertNotNull(firstResult, "第一次查询结果不应为空");
        System.out.println("第一次查询结果：总记录数 = " + firstResult.getTotalElements());

        // 记录第一次耗时
        double firstQueryTime = stopWatch.getLastTaskTimeMillis();
        System.out.printf("第一次查询耗时：%.2f ms%n", firstQueryTime);

        // ============ 第二次查询（命中缓存） ============
        stopWatch.start("第二次查询 - 命中缓存");
        var secondResult = userService.findUsers(pageable, UserConstants.USER_QUERY_ALL);
        stopWatch.stop();

        // 验证第二次查询结果与第一次一致
        assertNotNull(secondResult, "第二次查询结果不应为空");
        assertEquals(
            firstResult.getTotalElements(),
            secondResult.getTotalElements(),
            "两次查询结果的总记录数应相同"
        );

        // 记录第二次耗时
        double secondQueryTime = stopWatch.getLastTaskTimeMillis();
        System.out.printf("第二次查询耗时：%.2f ms%n", secondQueryTime);

        // ============ 验证性能提升 ============
        System.out.println("\n========== 性能对比 ==========");
        System.out.printf("第一次查询（无缓存）: %.2f ms%n", firstQueryTime);
        System.out.printf("第二次查询（命中缓存）: %.2f ms%n", secondQueryTime);

        // 验证第二次查询命中缓存（耗时显著降低）
        // 通常命中Redis缓存的查询时间应在1-10ms
        assertTrue(
            secondQueryTime < 100,
            String.format("第二次查询应命中缓存且耗时<100ms，实际耗时: %.2f ms", secondQueryTime)
        );

        // 计算性能提升倍数
        double improvement = firstQueryTime / secondQueryTime;
        System.out.printf("性能提升倍数: %.2fx%n", improvement);

        // 打印完整的StopWatch报告
        System.out.println("\n========== StopWatch完整报告 ==========");
        System.out.println(stopWatch.prettyPrint());
    }

    @Test
    @DisplayName("测试Product查询缓存性能 - 验证热点数据缓存效果")
    void testProductCachePerformance() {
        // 创建分页参数
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Direction.ASC, "price")
        );

        StopWatch stopWatch = new StopWatch();

        // ============ 第一次查询（无缓存或冷缓存） ============
        stopWatch.start("Product第一次查询");
        var firstResult = productService.findProducts(
            pageable,
            ProductConstants.PRODUCT_QUERY_ACTIVE_FOR_SALE
        );
        stopWatch.stop();

        assertNotNull(firstResult);
        double firstTime = stopWatch.getLastTaskTimeMillis();
        System.out.printf("Product第一次查询耗时：%.2f ms%n", firstTime);

        // ============ 第二次查询（应命中缓存） ============
        stopWatch.start("Product第二次查询");
        var secondResult = productService.findProducts(
            pageable,
            ProductConstants.PRODUCT_QUERY_ACTIVE_FOR_SALE
        );
        stopWatch.stop();

        assertNotNull(secondResult);
        assertEquals(firstResult.getTotalElements(), secondResult.getTotalElements());

        double secondTime = stopWatch.getLastTaskTimeMillis();
        System.out.printf("Product第二次查询耗时：%.2f ms%n", secondTime);

        // ============ 验证缓存性能 ============
        assertTrue(
            secondTime < 100,
            String.format("Product第二次查询应命中缓存且耗时<100ms，实际: %.2f ms", secondTime)
        );

        System.out.println("\n========== Product性能对比 ==========");
        System.out.printf("第一次: %.2f ms | 第二次: %.2f ms | 提升: %.2fx%n",
            firstTime, secondTime, firstTime / secondTime);
    }

    @Test
    @DisplayName("测试相同查询条件多次执行 - 验证缓存一致性")
    void testCacheConsistency() {
        Pageable pageable = PageRequest.of(0, 10);
        StopWatch stopWatch = new StopWatch();

        // 执行5次相同查询
        for (int i = 1; i <= 5; i++) {
            stopWatch.start("第" + i + "次查询");
            var result = userService.findUsers(pageable, UserConstants.USER_QUERY_ALL);
            stopWatch.stop();

            assertNotNull(result);
            System.out.printf("第%d次查询耗时: %.2f ms%n",
                i, stopWatch.getLastTaskTimeMillis());

            // 第2-5次查询都应该命中缓存且耗时<100ms
            if (i > 1) {
                assertTrue(
                    stopWatch.getLastTaskTimeMillis() < 100,
                    String.format("第%d次查询应命中缓存，耗时: %.2f ms", i, stopWatch.getLastTaskTimeMillis())
                );
            }
        }

        System.out.println("\n========== 多次查询性能报告 ==========");
        System.out.println(stopWatch.prettyPrint());
    }

    @Test
    @DisplayName("测试不同查询条件的缓存隔离性")
    void testCacheIsolation() {
        StopWatch stopWatch = new StopWatch();

        // 查询1：第一页
        Pageable pageable1 = PageRequest.of(0, 10);
        stopWatch.start("查询第一页");
        userService.findUsers(pageable1, UserConstants.USER_QUERY_ALL);
        stopWatch.stop();
        double time1 = stopWatch.getLastTaskTimeMillis();

        // 查询2：第二页（不同条件，应独立缓存）
        Pageable pageable2 = PageRequest.of(1, 10);
        stopWatch.start("查询第二页（首次）");
        userService.findUsers(pageable2, UserConstants.USER_QUERY_ALL);
        stopWatch.stop();
        double time2First = stopWatch.getLastTaskTimeMillis();

        // 再次查询第二页（应命中缓存）
        stopWatch.start("查询第二页（第二次）");
        userService.findUsers(pageable2, UserConstants.USER_QUERY_ALL);
        stopWatch.stop();
        double time2Second = stopWatch.getLastTaskTimeMillis();

        // 验证第二次查询第二页命中缓存
        assertTrue(time2Second < 100,
            String.format("第二页第二次查询应命中缓存，耗时: %.2f ms", time2Second));

        System.out.println("\n========== 缓存隔离性测试 ==========");
        System.out.printf("第一页查询: %.2f ms%n", time1);
        System.out.printf("第二页首次: %.2f ms%n", time2First);
        System.out.printf("第二页二次: %.2f ms （应命中缓存）%n", time2Second);
    }
}
