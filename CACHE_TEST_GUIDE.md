# Redis缓存性能测试指南

## 概述

本文档介绍如何运行Redis缓存性能测试，验证缓存命中率和性能提升效果。

## 测试类说明

### RedisCachePerformanceTest

**位置**：`src/test/java/com/example/demo/cache/RedisCachePerformanceTest.java`

**功能**：使用Spring的StopWatch记录查询耗时，验证缓存是否生效以及性能提升效果。

## 测试环境要求

### 1. 启动Redis服务

```bash
# macOS
brew services start redis

# Linux
sudo systemctl start redis

# Windows
redis-server.exe
```

验证Redis是否运行：
```bash
redis-cli ping
# 应返回: PONG
```

### 2. 配置应用

确保 `application.yml` 中的Redis配置正确：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:  # 如果有密码请填写
```

### 3. 准备测试数据

测试会自动使用内存数据库（H2），首次查询时如果没有数据会自动创建。

## 运行测试

### 方式1：使用Maven命令行

```bash
# 运行所有缓存性能测试
./mvnw test -Dtest=RedisCachePerformanceTest

# 运行特定测试方法
./mvnw test -Dtest=RedisCachePerformanceTest#testUserCachePerformance
./mvnw test -Dtest=RedisCachePerformanceTest#testProductCachePerformance

# 使用详细日志输出
./mvnw test -Dtest=RedisCachePerformanceTest -X
```

### 方式2：使用IDE

在IntelliJ IDEA或Eclipse中：

1. 右键点击 `RedisCachePerformanceTest.java`
2. 选择 "Run RedisCachePerformanceTest"
3. 或选择特定测试方法运行

## 测试方法详解

### 1. testUserCachePerformance

**测试目标**：验证用户查询的缓存性能

**测试步骤**：
1. 第一次查询所有用户（无缓存）→ 记录耗时
2. 第二次查询相同条件（命中缓存）→ 记录耗时
3. 验证第二次查询耗时 < 100ms
4. 输出性能对比报告

**预期结果**：
```
第一次查询结果：总记录数 = X
第一次查询耗时：50-200 ms（访问数据库）
第二次查询耗时：1-10 ms（命中Redis缓存）
性能提升倍数: 5-50x
```

### 2. testProductCachePerformance

**测试目标**：验证商品查询的缓存性能

**测试步骤**：
1. 查询可销售的商品（无缓存）→ 记录耗时
2. 再次查询相同条件（命中缓存）→ 记录耗时
3. 验证第二次查询耗时 < 100ms

**预期结果**：
```
Product第一次查询耗时：80-250 ms（访问数据库）
Product第二次查询耗时：1-10 ms（命中Redis缓存）
```

### 3. testCacheConsistency

**测试目标**：验证相同查询条件多次执行都能命中缓存

**测试步骤**：
1. 连续执行5次相同查询
2. 第2-5次都应该命中缓存且耗时 < 100ms

**预期结果**：
```
第1次查询耗时: 50-200 ms
第2次查询耗时: 1-10 ms（命中缓存✓）
第3次查询耗时: 1-10 ms（命中缓存✓）
第4次查询耗时: 1-10 ms（命中缓存✓）
第5次查询耗时: 1-10 ms（命中缓存✓）
```

### 4. testCacheIsolation

**测试目标**：验证不同查询条件的缓存隔离性

**测试步骤**：
1. 查询第一页数据
2. 查询第二页数据（首次，无缓存）
3. 再次查询第二页数据（应命中缓存）

**预期结果**：
```
第一页查询: 50-200 ms
第二页首次: 50-200 ms（不同条件）
第二页二次: 1-10 ms（命中缓存✓）
```

## 测试结果解读

### 成功测试输出示例

```bash
========== 性能对比 ==========
第一次查询（无缓存）: 156.32 ms
第二次查询（命中缓存）: 3.45 ms
性能提升倍数: 45.31x

========== StopWatch完整报告 ==========
StopWatch '': running time = 159773500 ns
---------------------------------------------
ms     %     Task name
---------------------------------------------
15632  098%  第一次查询 - 无缓存
00345  002%  第二次查询 - 命中缓存
```

### 关键指标

| 指标 | 预期范围 | 说明 |
|------|---------|------|
| 第一次查询 | 50-500 ms | 访问数据库，含SQL执行、网络传输 |
| 第二次查询 | 1-50 ms | 命中Redis缓存，主要耗时在网络传输 |
| 性能提升 | 5-100x | 取决于数据库查询复杂度 |
| 缓存命中率 | >90% | 第二次及后续查询均命中缓存 |

### 测试失败场景

#### 场景1：Redis未启动
```
错误: Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException

解决: 启动Redis服务
redis-server
```

#### 场景2：第二次查询耗时 > 100ms
```
错误: 第二次查询应命中缓存且耗时<100ms，实际耗时: 156.32 ms

可能原因:
1. Redis连接缓慢（网络问题）
2. 缓存未正确配置
3. 序列化/反序列化耗时过长

排查步骤:
1. 检查日志中是否有缓存命中提示
2. 使用redis-cli验证缓存是否存在
3. 检查Redis配置（连接池、超时设置）
```

## 监控Redis缓存

### 查看缓存Key

```bash
# 连接Redis
redis-cli

# 查看所有用户相关的缓存
KEYS "cache:saas:userListCache:*"

# 查看特定缓存内容
GET "cache:saas:userListCache:allUsers:0:10:..."

# 查看缓存数量
DBSIZE
```

### 查看缓存命中率

```bash
# 查看Redis状态
INFO stats

# 关键指标
keyspace_hits: X      # 缓存命中数
keyspace_misses: Y     # 缓存未命中数

命中率 = keyspace_hits / (keyspace_hits + keyspace_misses)
```

### 清空缓存后重新测试

```bash
# 清空所有缓存
redis-cli FLUSHALL

# 或只清空应用相关的缓存
redis-cli KEYS "cache:saas:*" | xargs redis-cli DEL
```

## 性能优化建议

如果测试结果显示缓存未生效或性能提升不明显，可以尝试以下优化：

### 1. 启用Redis日志

在 `application.yml` 中添加：
```yaml
logging:
  level:
    org.springframework.cache: DEBUG
    org.springframework.data.redis: DEBUG
```

重启应用后观察日志：
```
DEBUG CacheAspect: Cache entry for key 'allUsers:0:10' found in cache 'userListCache'
```

### 2. 检查缓存Key生成

确保缓存Key的生成逻辑正确：
```java
@Cacheable(
    key = "#strategyName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
)
```

相同条件应生成相同的Key。

### 3. 优化Redis连接

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20      # 增加连接数
          max-wait: 2000ms      # 增加等待时间
```

### 4. 使用本地缓存

对于极度热点数据，可以增加一级本地缓存（Caffeine）：
```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheSpecification("maximumSize=1000,expireAfterWrite=60s");
        return cacheManager;
    }
}
```

## 预期性能提升

根据实际测试，预期性能提升如下：

| 查询类型 | 无缓存 | 有缓存 | 提升倍数 |
|---------|-------|-------|--------|
| 简单查询 | 50-100ms | 1-5ms | 10-50x |
| 复杂查询 | 200-500ms | 5-10ms | 20-100x |
| 列表查询 | 100-300ms | 5-15ms | 10-30x |
| 分页查询 | 150-400ms | 5-20ms | 15-40x |

## 注意事项

1. **确保Redis已启动**：测试前请确认Redis服务正在运行
2. **单独运行测试**：避免与其他测试同时运行，以免影响结果
3. **多次运行取平均**：建议运行3-5次取平均值
4. **考虑数据库预热**：第一次查询可能包含JPA初始化时间
5. **网络延迟**：如果Redis和应用不在同一服务器，网络延迟会影响结果

## 总结测试流程

```bash
# 1. 启动Redis
redis-server

# 2. 启动应用（可选）
./mvnw spring-boot:run

# 3. 运行测试
./mvnw test -Dtest=RedisCachePerformanceTest

# 4. 查看结果
# 预期：第一次查询50-500ms，第二次查询<100ms
```

如果测试通过，说明Redis缓存已正确集成并生效！
