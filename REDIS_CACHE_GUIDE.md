# Redis缓存集成指南

## 概述

本指南介绍了如何在SaaS基础系统中集成Redis缓存，包括：

1. 配置Redis连接和缓存管理器
2. 使用Spring缓存注解（@Cacheable、@CacheEvict）
3. Redis工具类的使用
4. 缓存穿透和击穿的解决方案

---

## 快速开始

### 1. 安装Redis

#### macOS (使用Homebrew)
```bash
brew install redis
brew services start redis
```

#### Linux (Ubuntu)
```bash
sudo apt update
sudo apt install redis-server
sudo systemctl start redis
```

#### Windows
下载并安装 [Redis for Windows](https://github.com/microsoftarchive/redis/releases)

### 2. 验证Redis安装

```bash
redis-cli ping
# 应该返回 PONG

# 测试SET/GET
redis-cli
127.0.0.1:6379> set test "Hello Redis"
OK
127.0.0.1:6379> get test
"Hello Redis"
```

### 3. 配置应用

Redis配置已在 `application.yml` 中完成，默认参数：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:  # 如果有密码请配置
      database: 0  # 数据库编号（0-15）
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8  # 最大连接数
          max-idle: 8      # 最大空闲连接
          min-idle: 0      # 最小空闲连接
          max-wait: 1000ms # 最大等待时间
```

### 4. 启动应用

```bash
./mvnw spring-boot:run
```

应用启动后，Redis缓存会自动生效。

---

## 缓存配置详解

### 1. Redis配置类 (`RedisConfig.java`)

#### RedisTemplate配置

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // 使用Jackson2JsonRedisSerializer进行序列化
    Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    objectMapper.activateDefaultTyping(
        objectMapper.getPolymorphicTypeValidator(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );
    // 注册Java时间模块，支持LocalDateTime
    objectMapper.registerModule(new JavaTimeModule());
    serializer.setObjectMapper(objectMapper);

    // 设置Key和Value的序列化方式
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);

    return template;
}
```

**特点：**
- 使用JSON格式存储数据，可读性好
- 支持LocalDateTime等Java 8时间类型
- 自动处理多态类型

#### CacheManager配置

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // 默认缓存配置（10分钟过期）
    RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMillis(productTtl))
        .disableCachingNullValues()
        .prefixCacheNameWith("cache:saas:")
        .serializeKeysWith(...)
        .serializeValuesWith(...);

    // 自定义缓存配置
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // 用户缓存（5分钟）
    cacheConfigurations.put("userCache", ...);

    // 商品缓存（10分钟）
    cacheConfigurations.put("productCache", ...);

    // 用户列表缓存（2分钟）
    cacheConfigurations.put("userListCache", ...);

    // 空值缓存（1分钟，用于防穿透）
    cacheConfigurations.put("nullValueCache", ...);

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultCacheConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

**缓存过期时间配置：**

| 缓存名称 | 过期时间 | 说明 |
|---------|---------|------|
| `userCache` | 5分钟 | 单个用户信息 |
| `productCache` | 10分钟 | 单个商品信息 |
| `userListCache` | 2分钟 | 用户列表 |
| `productListCache` | 2分钟 | 商品列表 |
| `nullValueCache` | 1分钟 | 空值缓存（防穿透） |

---

## 使用Spring缓存注解

### @Cacheable - 查询并缓存结果

#### 示例：查询用户信息

```java
@Service
@CacheConfig(cacheNames = "userCache")
public class UserServiceImpl implements UserService {

    @Override
    @Cacheable(
        value = "userListCache",
        key = "#strategyName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString().hashCode()",
        unless = "#result == null || #result.getTotalElements() == 0"
    )
    public Page<User> findUsers(Pageable pageable, String strategyName, Object... params) {
        // 查询逻辑
    }
}
```

**注解参数说明：**

- `value`: 缓存名称，对应 `RedisCacheConfiguration`
- `key`: SpEL表达式，生成缓存Key
  - 格式：`strategyName:page:size:sortHash`
  - 确保相同查询条件的请求命中同一缓存
- `unless`: 条件表达式，满足条件时不缓存
  - `#result == null`: 不缓存null值
  - `#result.getTotalElements() == 0`: 不缓存空结果页

#### 示例：查询商品信息

```java
@Service
@CacheConfig(cacheNames = "productCache")
public class ProductServiceImpl implements ProductService {

    @Override
    @Cacheable(
        value = "productListCache",
        key = "#strategyName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString().hashCode()",
        unless = "#result == null || #result.getTotalElements() == 0"
    )
    public Page<Product> findProducts(Pageable pageable, String strategyName, Object... params) {
        // 查询逻辑
    }
}
```

### @CacheEvict - 清理缓存

#### 示例：保存用户时清理缓存

```java
@CacheEvict(value = "userListCache", allEntries = true, beforeInvocation = true)
public void saveUser(User user) {
    // 保存逻辑
}
```

**参数说明：**

- `value`: 缓存名称，多个用数组 `{"cache1", "cache2"}`
- `allEntries = true`: 清理该缓存的所有条目
- `beforeInvocation = true`: 在方法执行前清理（防止方法失败导致缓存未清理）

#### 示例：管理库存时清理缓存

```java
@CacheEvict(value = {"productListCache", "productCache"}, allEntries = true, beforeInvocation = true)
public void manageStock(Long productId, Integer quantity) {
    // 库存管理逻辑
}
```

### @CachePut - 更新缓存

```java
@CachePut(value = "userCache", key = "#user.id")
public User updateUser(User user) {
    // 更新逻辑
    return updatedUser;
}
```

---

## Redis工具类使用

### RedisCacheUtils功能概览

位于 `util/RedisCacheUtils.java`

#### 基本操作

```java
@Component
public class RedisCacheUtils {

    // 获取缓存值
    public Object get(String key)

    // 设置缓存值（带过期时间）
    public boolean put(String key, Object value, long ttl)

    // 删除缓存
    public boolean delete(String key)

    // 检查缓存是否存在
    public boolean exists(String key)

    // 设置过期时间
    public boolean expire(String key, long ttl)

    // 原子递增/递减
    public Long increment(String key, long delta)
    public Long decrement(String key, long delta)
}
```

#### 使用示例

```java
@Service
public class SomeService {

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    public void example() {
        // 设置缓存（1小时过期）
        User user = new User("john", "password", Role.USER);
        redisCacheUtils.put("user:1", user, 3600);

        // 获取缓存
        User cachedUser = (User) redisCacheUtils.get("user:1");

        // 删除缓存
        redisCacheUtils.delete("user:1");

        // 检查是否存在
        boolean exists = redisCacheUtils.exists("user:1");

        // 原子递增（计数器）
        Long count = redisCacheUtils.increment("user:visit:1", 1);
    }
}
```

---

## 缓存问题解决方案

### 1. 缓存穿透（Cache Penetration）

**问题描述：**
查询一个不存在的数据，由于缓存中没有，所有请求都会打到数据库，造成数据库压力。

**解决方案：**
缓存空值（NULL_VALUE），并设置较短的过期时间。

#### 工具类方法：`getWithPenetrationProtection`

```java
/**
 * 获取缓存值，带防穿透机制
 * 如果缓存中不存在，查询数据库，并缓存结果（包括null）
 */
public <T> T getWithPenetrationProtection(String key, long ttl, DbLoader<T> dbLoader) {
    // 1. 查询缓存
    Object cacheValue = get(key);

    // 2. 缓存命中
    if (cacheValue != null) {
        if (NULL_VALUE.equals(cacheValue)) {
            return null;  // 数据库中不存在
        }
        return (T) cacheValue;
    }

    // 3. 缓存未命中，查询数据库
    T dbValue = dbLoader.load();

    // 4. 缓存结果（包括null）
    if (dbValue == null) {
        put(key, NULL_VALUE, 60);  // 空值缓存1分钟
    } else {
        put(key, dbValue, ttl);
    }

    return dbValue;
}
```

#### 使用示例

```java
public User findUserById(Long userId) {
    String cacheKey = redisCacheUtils.generateKey("user", userId);

    return redisCacheUtils.getWithPenetrationProtection(
        cacheKey,
        300,  // 5分钟过期
        () -> {
            // 数据库查询逻辑
            return userRepository.findById(userId).orElse(null);
        }
    );
}
```

### 2. 缓存击穿（Cache Breakdown）

**问题描述：**
热点数据在缓存过期瞬间，大量请求同时打到数据库，造成数据库压力剧增。

**解决方案：**
使用互斥锁（SETNX），只有一个请求能查询数据库并更新缓存，其他请求等待。

#### 工具类方法：`getWithBreakdownProtection`

```java
/**
 * 获取缓存值，带防击穿机制（互斥锁）
 * 使用Redis的SETNX命令实现分布式锁
 */
public <T> T getWithBreakdownProtection(String key, String lockKey, long ttl, long lockTtl, DbLoader<T> dbLoader) {
    // 1. 查询缓存
    Object cacheValue = get(key);
    if (cacheValue != null) {
        return (T) cacheValue;
    }

    // 2. 缓存未命中，尝试获取锁
    boolean lockAcquired = putIfAbsent(lockKey, "LOCKED", lockTtl);

    if (lockAcquired) {
        try {
            // 3. 获取锁成功，查询数据库
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

#### 使用示例

```java
public User findHotUser(Long userId) {
    String cacheKey = redisCacheUtils.generateKey("user", userId);
    String lockKey = redisCacheUtils.generateLockKey("user:" + userId);

    return redisCacheUtils.getWithBreakdownProtection(
        cacheKey,
        lockKey,
        300,   // 缓存5分钟
        30,    // 锁30秒过期
        () -> userRepository.findById(userId).orElse(null)
    );
}
```

### 3. 缓存雪崩（Cache Avalanche）

**问题描述：**
大量缓存同时过期，导致所有请求打到数据库。

**解决方案：**
1. 设置随机的过期时间
2. 热点数据永不过期（后台异步更新）
3. 使用多级缓存（本地缓存 + Redis）

#### 实现方式（在配置中）

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMillis(productTtl))
        .disableCachingNullValues();

    // 为不同缓存设置不同过期时间，避免同时过期
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // 用户缓存：5分钟 + 0-60秒随机
    cacheConfigurations.put("userCache",
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMillis(userTtl + (long)(Math.random() * 60000)))
    );

    // 商品缓存：10分钟 + 0-120秒随机
    cacheConfigurations.put("productCache",
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMillis(productTtl + (long)(Math.random() * 120000)))
    );

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultCacheConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

---

## 缓存Key生成策略

### 工具类方法

```java
/**
 * 生成缓存Key
 * 格式：cache:sass:{prefix}:{identifier}
 */
public String generateKey(String prefix, Object identifier) {
    return String.format("cache:saas:%s:%s", prefix, identifier);
}

// 使用示例
String userKey = redisCacheUtils.generateKey("user", userId);
// 结果：cache:saas:user:1

/**
 * 生成列表缓存Key
 * 格式：cache:sass:{prefix}:list:{page}:{size}:{sortBy}:{sortDirection}
 */
public String generateListKey(String prefix, int page, int size, String sortBy, String sortDirection) {
    return String.format("cache:saas:%s:list:%d:%d:%s:%s",
        prefix, page, size, sortBy, sortDirection);
}

// 使用示例
String userListKey = redisCacheUtils.generateListKey(
    "user",
    0,      // page
    10,     // size
    "createdAt",
    "desc"
);
// 结果：cache:saas:user:list:0:10:createdAt:desc

/**
 * 生成分布式锁Key
 */
public String generateLockKey(String key) {
    return String.format("lock:saas:%s", key);
}
```

---

## 监控和调试

### 查看缓存内容

```bash
# 连接Redis
redis-cli

# 查看所有键
KEYS cache:saas:*

# 查看特定缓存
GET "cache:saas:userListCache:userById:0:10:createdAt:desc:12345"

# 查看过期时间
TTL "cache:saas:userListCache:..."

# 查看缓存统计
INFO keyspace
```

### 查看Spring缓存日志

在 `application.yml` 中配置：

```yaml
logging:
  level:
    org.springframework.cache: DEBUG
    org.springframework.data.redis: DEBUG
```

日志示例：
```
DEBUG o.s.c.a.AnnotationCacheAspect - Computed cache key 'userById:0:10:...[User@1a2b3c4d]'
DEBUG o.s.c.i.CacheInterceptor - Cache entry for key 'userById:...' found in cache 'userListCache'
```

---

## 性能优化建议

### 1. 批量操作

```java
// 批量获取
List<String> keys = Arrays.asList("user:1", "user:2", "user:3");
List<Object> values = redisCacheUtils.multiGet(keys);

// 批量设置
Map<String, Object> map = new HashMap<>();
map.put("user:1", user1);
map.put("user:2", user2);
redisCacheUtils.multiPut(map, 3600);
```

### 2. 管道操作（Pipeline）

对于大量操作，使用管道减少网络往返：

```java
redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (int i = 0; i < 1000; i++) {
        connection.set(("key:" + i).getBytes(), ("value:" + i).getBytes());
    }
    return null;
});
```

### 3. 缓存预热

在系统启动时，将热点数据加载到缓存：

```java
@Component
public class CacheWarmUp implements ApplicationRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        // 预热热点用户数据
        Pageable pageable = PageRequest.of(0, 100);
        Page<User> users = userService.findUsers(pageable, "allUsers");
        System.out.println("缓存预热完成，加载了 " + users.getTotalElements() + " 个用户");
    }
}
```

### 4. 热点数据永不过期

对于极度热点的数据，设置永不过期，后台异步更新：

```java
@Cacheable(value = "hotProductCache", key = "#productId")
public Product getHotProduct(Long productId) {
    return productRepository.findById(productId).orElse(null);
}

// 定时任务更新热点数据
@Scheduled(fixedRate = 60000)  // 每分钟更新一次
public void updateHotProducts() {
    List<Long> hotProductIds = Arrays.asList(1L, 2L, 3L);
    for (Long id : hotProductIds) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            redisCacheUtils.put("product:" + id, product, 0);  // 0表示永不过期
        }
    }
}
```

---

## 常见问题

### Q1: 如何清理所有缓存？

```java
public void clearAllCache() {
    // 清理用户缓存
    redisCacheUtils.deleteByPattern("cache:saas:userCache:*");

    // 清理商品缓存
    redisCacheUtils.deleteByPattern("cache:saas:productCache:*");

    // 或者使用CacheManager
    cacheManager.getCacheNames().forEach(name -> {
        cacheManager.getCache(name).clear();
    });
}
```

### Q2: 缓存未生效怎么办？

1. 检查Redis是否启动：
   ```bash
   redis-cli ping
   ```

2. 检查缓存注解是否启用：
   ```java
   @EnableCaching  // 确保有这个注解
   ```

3. 检查日志是否显示缓存命中。

4. 检查Key是否正确生成。

### Q3: 如何监控缓存命中率？

```java
@Autowired
private RedisCacheWriter redisCacheWriter;

public void printStats() {
    String stats = redisCacheUtils.info();
    System.out.println(stats);
}
```

---

## 最佳实践

### ✅ 应该缓存什么？

- 热点数据（访问频繁）
- 计算开销大的数据
- 不经常变化的数据
- 列表数据（适度缓存）

### ❌ 不应该缓存什么？

- 实时性要求高的数据（如库存）
- 敏感数据（密码、token）
- 大数据量（占用内存）
- 频繁变更的数据

### 📌 建议

1. **合理设置TTL**：根据数据变更频率设置合适的过期时间
2. **使用批量操作**：减少网络往返
3. **监控缓存命中率**：低于80%需要优化
4. **缓存预热**：启动时加载热点数据
5. **异常处理**：Redis异常时降级到数据库
6. **Key设计**：包含业务标识和版本号
7. **分布式锁**：防止缓存击穿
8. **空值缓存**：防止缓存穿透

---

## Redis命令速查

```bash
# 连接Redis
redis-cli

# 查看所有键
KEYS *

# 查看匹配模式的键
KEYS "cache:saas:*"

# 查看键的类型
TYPE "cache:saas:user:1"

# 查看值
GET "cache:saas:user:1"

# 查看过期时间
TTL "cache:saas:user:1"
# -1: 永不过期
# -2: 键不存在
# 正数: 剩余秒数

# 删除键
DEL "cache:saas:user:1"

# 批量删除
redis-cli KEYS "cache:saas:user:*" | xargs redis-cli DEL

# 查看内存使用
INFO memory

# 查看缓存统计
INFO keyspace

# 清空所有数据（危险操作）
FLUSHALL
```

---

## 总结

本指南涵盖了Redis缓存在Spring Boot项目中的完整集成和使用方法，包括：

✅ Redis配置和连接
✅ Spring缓存注解使用
✅ Redis工具类封装
✅ 缓存穿透/击穿解决方案
✅ 性能优化建议
✅ 监控和调试方法

使用缓存可以大幅提升系统性能，但需要合理设计和监控，避免缓存问题影响业务。
