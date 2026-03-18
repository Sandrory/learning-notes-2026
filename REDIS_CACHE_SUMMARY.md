# Redis缓存实现总结

## 📊 实现概览

### 1. Redis依赖集成

**文件：** `pom.xml`

```xml
<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 缓存支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- JSON序列化（支持Java 8时间） -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

### 2. Redis配置

**文件：** `src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: 1000ms
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 默认10分钟
      use-key-prefix: true
      key-prefix: cache:saas
      cache-null-values: true
```

**文件：** `src/main/java/com/example/demo/config/RedisConfig.java`

- ✅ RedisTemplate配置（JSON序列化）
- ✅ CacheManager配置（多种缓存过期时间）
- ✅ StringRedisTemplate Bean
- ✅ RedisCacheWriter Bean

### 3. 缓存工具类

**文件：** `src/main/java/com/example/demo/util/RedisCacheUtils.java`

**代码行数：** 520行

**功能模块：**

#### 基础操作（100+行）
- `get()` - 获取缓存值
- `put()` - 设置缓存值（带TTL）
- `delete()` - 删除缓存
- `exists()` - 检查缓存是否存在
- `expire()` - 设置过期时间
- `increment()` / `decrement()` - 原子递增/递减

#### 高级操作（150+行）
- `getAndSet()` - 获取并设置
- `putIfAbsent()` - 设置不存在时的值
- `multiGet()` / `multiPut()` - 批量操作
- `keys()` - 扫描匹配键
- `info()` - 获取Redis服务器信息

#### 防缓存穿透（100+行）

**方法：** `getWithPenetrationProtection()`

**实现原理：**
```java
public <T> T getWithPenetrationProtection(String key, long ttl, DbLoader<T> dbLoader) {
    // 1. 查询缓存
    Object cacheValue = get(key);

    // 2. 缓存命中
    if (cacheValue != null) {
        if (NULL_VALUE.equals(cacheValue)) {
            return null;  // 数据库中不存在（防穿透）
        }
        return (T) cacheValue;
    }

    // 3. 缓存未命中，查询数据库
    T dbValue = dbLoader.load();

    // 4. 缓存结果（包括null→防穿透）
    if (dbValue == null) {
        put(key, NULL_VALUE, 60);  // 空值缓存1分钟
    } else {
        put(key, dbValue, ttl);
    }

    return dbValue;
}
```

**使用示例：**
```java
String cacheKey = redisCacheUtils.generateKey("user", userId);

User user = redisCacheUtils.getWithPenetrationProtection(
    cacheKey,
    300,  // 5分钟过期
    () -> userRepository.findById(userId).orElse(null)
);
```

#### 防缓存击穿（150+行）

**方法：** `getWithBreakdownProtection()`

**实现原理（互斥锁）：**
```java
public <T> T getWithBreakdownProtection(String key, String lockKey, long ttl, long lockTtl, DbLoader<T> dbLoader) {
    // 1. 查询缓存
    Object cacheValue = get(key);
    if (cacheValue != null) {
        return (T) cacheValue;
    }

    // 2. 尝试获取分布式锁（SETNX）
    boolean lockAcquired = putIfAbsent(lockKey, "LOCKED", lockTtl);

    if (lockAcquired) {
        try {
            // 3. 查询数据库
            T dbValue = dbLoader.load();

            // 4. 缓存结果
            if (dbValue == null) {
                put(key, NULL_VALUE, 60);
            } else {
                put(key, dbValue, ttl);
            }

            return dbValue;
        } finally {
            // 5. 释放锁
            delete(lockKey);
        }
    } else {
        // 6. 获取锁失败，等待并重试
        Thread.sleep(100);
        return getWithBreakdownProtection(key, lockKey, ttl, lockTtl, dbLoader);
    }
}
```

**使用示例：**
```java
String cacheKey = redisCacheUtils.generateKey("hotProduct", productId);
String lockKey = redisCacheUtils.generateLockKey("product:" + productId);

Product product = redisCacheUtils.getWithBreakdownProtection(
    cacheKey,
    lockKey,
    300,   // 缓存5分钟
    30,    // 锁30秒过期
    () -> productRepository.findById(productId).orElse(null)
);
```

### 4. Service层集成

**文件：** `UserServiceImpl.java` + `ProductServiceImpl.java`

#### @Cacheable 注解使用

```java
@Override
@Cacheable(
    value = "userListCache",
    key = "#strategyName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString().hashCode()",
    unless = "#result == null || #result.getTotalElements() == 0"
)
public Page<User> findUsers(Pageable pageable, String strategyName, Object... params) {
    // 查询逻辑
}
```

**缓存Key设计：**
- `userListCache:userById:0:10:createdAt:desc:12345`
- `productListCache:allProducts:0:20:price:desc:67890`

#### @CacheEvict 注解使用

```java
@CacheEvict(value = "userListCache", allEntries = true, beforeInvocation = true)
public void saveUser(User user) {
    // 保存逻辑
}

@CacheEvict(value = {"productListCache", "productCache"}, allEntries = true, beforeInvocation = true)
public void manageStock(Long productId, Integer quantity) {
    // 库存管理逻辑
}
```

### 5. 缓存过期时间配置

| 缓存名称 | 过期时间 | 说明 |
|---------|---------|------|
| `userCache` | 5分钟 | 单个用户信息 |
| `productCache` | 10分钟 | 单个商品信息 |
| `userListCache` | 2分钟 | 用户列表 |
| `productListCache` | 2分钟 | 商品列表 |
| `nullValueCache` | 1分钟 | 空值缓存（防穿透） |

### 6. 文档

- ✅ `REDIS_CACHE_GUIDE.md` - 完整Redis缓存使用指南（400+行）
- ✅ `REDIS_CACHE_SUMMARY.md` - 本总结文档

---

## 📊 关键指标

### 代码统计

- **RedisConfig.java**: 120行
  - RedisTemplate配置（40行）
  - CacheManager配置（60行）
  - 辅助方法（20行）

- **RedisCacheUtils.java**: 520行
  - 基础操作（100行）
  - 高级操作（150行）
  - 防穿透机制（100行）
  - 防击穿机制（150行）
  - 其他方法（20行）

- **Service层注解**: 20行
  - @Cacheable: 2个方法
  - @CacheEvict: 2个方法

**总计Redis相关代码：** ~660行

### 性能指标（预估）

| 场景 | 无缓存 | 有缓存 | 提升 |
|------|-------|-------|------|
| 用户查询 | 50ms | 5ms | 10x |
| 商品列表 | 100ms | 10ms | 10x |
| 热点数据 | 50ms | 1ms | 50x |

---

## 🎯 功能清单

### ✅ Spring缓存注解集成

- [x] @Cacheable - 查询结果缓存
- [x] @CacheEvict - 缓存清理
- [x] @CacheConfig - 缓存配置
- [x] CacheManager - 多缓存配置

### ✅ Redis配置

- [x] RedisTemplate（JSON序列化）
- [x] CacheManager（TTL配置）
- [x] StringRedisTemplate
- [x] 连接池配置

### ✅ 防缓存穿透

- [x] 空值缓存机制
- [x] 短期过期策略
- [x] 自动降级处理
- [x] DbLoader函数式接口

### ✅ 防缓存击穿

- [x] 分布式锁实现（SETNX）
- [x] 互斥等待机制
- [x] 自动重试
- [x] 防死锁（TTL）

### ✅ Redis工具类

- [x] 基础操作（CRUD、过期）
- [x] 高级操作（批量、原子）
- [x] Key生成策略
- [x] 性能监控

### ✅ 文档

- [x] 完整使用指南
- [x] API示例
- [x] 问题解决方案
- [x] 最佳实践

---

## 🚀 使用示例

### 快速开始

```java
@Autowired
private UserService userService;

// 第一次查询，会访问数据库并缓存
Page<User> users1 = userService.findUsers(pageable, "allUsers");

// 第二次查询，直接从Redis缓存返回（TTL内）
Page<User> users2 = userService.findUsers(pageable, "allUsers"); // ❌ 不访问数据库
```

### 防缓存穿透

```java
@Autowired
private RedisCacheUtils redisCacheUtils;

public User findUserSafely(Long userId) {
    String cacheKey = redisCacheUtils.generateKey("user", userId);

    return redisCacheUtils.getWithPenetrationProtection(
        cacheKey,
        300,  // 5分钟
        () -> userRepository.findById(userId).orElse(null)
    );
}
```

### 防缓存击穿

```java
public Product findHotProductSafely(Long productId) {
    String cacheKey = redisCacheUtils.generateKey("hotProduct", productId);
    String lockKey = redisCacheUtils.generateLockKey("product:" + productId);

    return redisCacheUtils.getWithBreakdownProtection(
        cacheKey,
        lockKey,
        300,   // 缓存5分钟
        30,    // 锁30秒过期
        () -> productRepository.findById(productId).orElse(null)
    );
}
```

### 批量操作

```java
// 批量获取
List<String> keys = Arrays.asList("user:1", "user:2", "user:3");
List<Object> users = redisCacheUtils.multiGet(keys);

// 批量设置
Map<String, Object> map = new HashMap<>();
map.put("user:1", user1);
map.put("user:2", user2);
redisCacheUtils.multiPut(map, 3600);
```

### 监控缓存

```java
// 查看Redis信息
String info = redisCacheUtils.info();
System.out.println(info);

// 查看缓存数量
Long size = redisCacheUtils.size();
System.out.println("缓存数量: " + size);
```

---

## 💡 最佳实践

### ✅ 应该做的

1. **合理设置TTL**
   - 热点数据：5-10分钟
   - 冷数据：30分钟-1小时
   - 列表数据：1-2分钟

2. **使用合适的缓存Key**
   ```java
   // 好的Key
   "user:12345"

   // 不好的Key
   "user"  // 没有区分度
   ```

3. **批量操作代替多次单操作**
   ```java
   // ✅ 推荐
   redisCacheUtils.multiGet(keys);

   // ❌ 不推荐
   for (String key : keys) {
       redisCacheUtils.get(key);
   }
   ```

4. **异常处理**
   ```java
   try {
       return redisCacheUtils.get(key);
   } catch (Exception e) {
       // Redis异常时降级到数据库
       return loadFromDatabase();
   }
   ```

5. **缓存预热**
   ```java
   @PostConstruct
   public void warmUp() {
       // 启动时加载热点数据
       loadHotDataToCache();
   }
   ```

### ❌ 不应该做的

1. **缓存所有数据**
   - 不需要缓存冷数据
   - 节省内存空间

2. **缓存大对象**
   - 单个缓存项 < 1MB
   - 大对象拆分存储

3. **没有缓存清理策略**
   - 数据更新时清理缓存
   - 使用@CacheEvict

4. **缓存敏感数据**
   - 密码、token等
   - 个人隐私数据

5. **忽略缓存一致性**
   - 数据库更新后，缓存应失效
   - 使用事务保证一致性

---

## 🔍 故障排查

### 问题1：缓存未生效

**症状：** 每次请求都访问数据库

**排查步骤：**
1. 检查Redis是否启动
   ```bash
   redis-cli ping
   ```

2. 检查@EnableCaching是否存在
   ```java
   @SpringBootApplication
   @EnableCaching  // <--- 确保有这个注解
   public class DemoApplication {}
   ```

3. 查看日志
   ```yaml
   logging:
     level:
       org.springframework.cache: DEBUG
   ```

4. 检查缓存Key是否正确

### 问题2：缓存穿透

**症状：** 大量请求打到数据库，查询不存在的数据

**解决方案：**
```java
// 使用防穿透方法
redisCacheUtils.getWithPenetrationProtection(key, ttl, dbLoader);
```

### 问题3：缓存雪崩

**症状：** 大量缓存同时过期，数据库压力剧增

**解决方案：**
1. 设置随机过期时间
   ```java
   long ttl = 300 + (long)(Math.random() * 60);  // 300-360秒随机
   ```

2. 使用多级缓存
   ```
   Caffeine（本地） + Redis（远程）
   ```

3. 缓存预热
   ```java
   @Scheduled(fixedRate = 60000)
   public void warmUp() {
       // 定时预热热点数据
   }
   ```

### 问题4：Redis连接超时

**症状：** `RedisCommandTimeoutException`

**解决方案：**
1. 检查网络连接
2. 调整超时配置
   ```yaml
   spring:
     data:
       redis:
         timeout: 5000ms  # 增加到5秒
         connect-timeout: 5000ms
   ```

3. 增加连接池
   ```yaml
   lettuce:
     pool:
       max-active: 20
       max-wait: 2000ms
   ```

---

## 📝 总结

### Redis缓存集成完成度

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| RedisTemplate配置 | ✅ | JSON序列化，支持Java 8时间 |
| CacheManager配置 | ✅ | 多缓存，TTL配置 |
| @Cacheable注解 | ✅ | Service层集成 |
| @CacheEvict注解 | ✅ | 数据更新清理缓存 |
| 防缓存穿透 | ✅ | 空值缓存机制 |
| 防缓存击穿 | ✅ | 分布式互斥锁 |
| Redis工具类 | ✅ | 520行完整封装 |
| 使用文档 | ✅ | 详细指南 |
| 配置文档 | ✅ | application.yml |

### 核心优势

1. **开箱即用**
   - 依赖已添加
   - 配置已完成
   - 工具类已封装

2. **高性能**
   - JSON序列化
   - 连接池配置
   - 批量操作支持

3. **高可用**
   - 防穿透机制
   - 防击穿机制
   - 异常降级

4. **易扩展**
   - 策略模式
   - 工厂模式
   - 注解驱动

### 后续建议

1. **添加缓存监控面板**
   - RedisInsight
   - Spring Boot Admin

2. **实现多级缓存**
   - Caffeine + Redis

3. **添加缓存预热**
   - 启动时加载热点数据

4. **配置Redis集群**
   - 提高可用性
   - 横向扩展

---

**文档位置：**
- Redis使用指南：`REDIS_CACHE_GUIDE.md`
- 项目总结：`REDIS_CACHE_SUMMARY.md`（本文档）
- 数据库优化：`DATABASE_INDEX_OPTIMIZATION.md`
- 完整项目报告：`FINAL_SUMMARY.md`

**Redis配置位置：**
- 配置文件：`src/main/resources/application.yml`
- Java配置：`src/main/java/com/example/demo/config/RedisConfig.java`
- 工具类：`src/main/java/com/example/demo/util/RedisCacheUtils.java`

**最后更新：** 2026-03-18
