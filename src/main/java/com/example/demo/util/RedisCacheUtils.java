package com.example.demo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 * 封装Redis常用操作，提供防缓存穿透、击穿解决方案
 */
@Component
public class RedisCacheUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 缓存空值标识（用于防止缓存穿透）
     */
    private static final String NULL_VALUE = "__NULL__";

    /**
     * 默认锁过期时间（30秒）
     * 用于防止死锁
     */
    private static final long LOCK_EXPIRE_TIME = 30;

    /**
     * 获取缓存值
     * 解决缓存穿透：如果缓存中不存在，返回NULL_VALUE标识
     * 这样可以防止大量请求直接打到数据库
     *
     * @param key 缓存键
     * @return 缓存值，如果不存在返回null，如果存储的是NULL_VALUE标识返回NULL_VALUE
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            // Redis异常时，降级处理（不抛异常，让请求打到数据库）
            return null;
        }
    }

    /**
     * 设置缓存值
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间（秒）
     * @return 是否成功
     */
    public boolean put(String key, Object value, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置缓存值（无过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否成功
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 批量删除缓存（支持模式匹配）
     *
     * @param pattern 键的模式（如user:*）
     */
    public void deleteByPattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
        } catch (Exception e) {
            // 异常不抛出
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取缓存过期时间
     *
     * @param key 缓存键
     * @return 过期时间（秒），-1表示永不过期，-2表示键不存在
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            return -2L;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key  缓存键
     * @param ttl  过期时间（秒）
     * @return 是否成功
     */
    public boolean expire(String key, long ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, ttl, TimeUnit.SECONDS));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 原子递增
     *
     * @param key   缓存键
     * @param delta 增量
     * @return 递增后的值
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 原子递减
     *
     * @param key   缓存键
     * @param delta 减量
     * @return 递减后的值
     */
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取并设置（原子操作）
     *
     * @param key   缓存键
     * @param value 新值
     * @return 旧值
     */
    public Object getAndSet(String key, Object value) {
        try {
            return redisTemplate.opsForValue().getAndSet(key, value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置缓存值（仅当键不存在时）
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间（秒）
     * @return 是否设置成功（true表示键不存在并设置成功）
     */
    public boolean putIfAbsent(String key, Object value, long ttl) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ==================== 缓存穿透问题解决方案 ====================
     */

    /**
     * 获取缓存值，带防穿透机制
     * 如果缓存中不存在，返回特殊标识NULL_VALUE
     *
     * @param key      缓存键
     * @param ttl      空值缓存过期时间（秒）
     * @param dbLoader 数据库加载器（当缓存不存在时调用）
     * @return 缓存值，如果数据库也不存在返回NULL_VALUE
     * @param <T> 返回值类型
     */
    public <T> T getWithPenetrationProtection(String key, long ttl, DbLoader<T> dbLoader) {
        // 1. 查询缓存
        Object cacheValue = get(key);

        // 2. 缓存命中
        if (cacheValue != null) {
            // 如果是NULL_VALUE标识，表示数据库中不存在
            if (NULL_VALUE.equals(cacheValue)) {
                return null;
            }
            return (T) cacheValue;
        }

        // 3. 缓存未命中，查询数据库
        T dbValue = dbLoader.load();

        // 4. 数据库也不存在，缓存NULL_VALUE（防穿透）
        if (dbValue == null) {
            put(key, NULL_VALUE, 60);  // 空值缓存1分钟
            return null;
        }

        // 5. 数据库存在，缓存数据
        put(key, dbValue, ttl);
        return dbValue;
    }

    /**
     * 数据库加载器接口（用于防穿透）
     */
    @FunctionalInterface
    public interface DbLoader<T> {
        T load();
    }

    /**
     * ==================== 缓存击穿问题解决方案 ====================
     */

    /**
     * 获取缓存值，带防击穿机制（互斥锁）
     * 使用Redis的SETNX命令实现分布式锁
     *
     * @param key          缓存键
     * @param lockKey      锁键
     * @param ttl          缓存过期时间（秒）
     * @param lockTtl      锁过期时间（秒）
     * @param dbLoader     数据库加载器
     * @return 缓存值
     * @param <T> 返回值类型
     */
    public <T> T getWithBreakdownProtection(String key, String lockKey, long ttl, long lockTtl, DbLoader<T> dbLoader) {
        // 1. 查询缓存
        Object cacheValue = get(key);
        if (cacheValue != null) {
            return (T) cacheValue;
        }

        // 2. 缓存未命中，尝试获取锁
        boolean lockAcquired = false;
        try {
            lockAcquired = putIfAbsent(lockKey, "LOCKED", lockTtl);

            if (lockAcquired) {
                // 3. 获取锁成功，查询数据库
                T dbValue = dbLoader.load();

                // 4. 缓存数据库结果（即使为null也缓存，防击穿）
                if (dbValue == null) {
                    put(key, NULL_VALUE, 60);  // 空值缓存1分钟
                } else {
                    put(key, dbValue, ttl);
                }

                return dbValue;
            } else {
                // 5. 获取锁失败，等待并重试
                Thread.sleep(100);  // 等待100ms
                return getWithBreakdownProtection(key, lockKey, ttl, lockTtl, dbLoader);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            // 6. 释放锁（只有自己加的锁才能释放）
            if (lockAcquired) {
                delete(lockKey);
            }
        }
    }

    /**
     * ==================== 高级缓存操作 ====================
     */

    /**
     * 批量获取缓存值
     *
     * @param keys 缓存键列表
     * @return 缓存值列表
     */
    public java.util.List<Object> multiGet(java.util.List<String> keys) {
        try {
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 批量设置缓存值
     *
     * @param map 键值对Map
     * @param ttl 过期时间（秒）
     */
    public void multiPut(java.util.Map<String, Object> map, long ttl) {
        try {
            redisTemplate.opsForValue().multiSet(map);
            // 为所有key设置过期时间
            for (String key : map.keySet()) {
                expire(key, ttl);
            }
        } catch (Exception e) {
            // 异常不抛出
        }
    }

    /**
     * ==================== 键管理 ====================
     */

    /**
     * 扫描匹配模式的键
     *
     * @param pattern 模式匹配
     * @return 键集合
     */
    public java.util.Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            return java.util.Collections.emptySet();
        }
    }

    /**
     * 获取所有缓存键的数量
     *
     * @return 键数量
     */
    public Long size() {
        try {
            return redisTemplate.countExistingKeys(keys("*"));
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * ==================== 性能监控 ====================
     */

    /**
     * 获取Redis服务器信息
     *
     * @return 服务器信息
     */
    public String info() {
        try {
            return redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                try {
                    return connection.serverCommands().info();
                } catch (Exception e) {
                    return "Error retrieving Redis info: " + e.getMessage();
                }
            });
        } catch (Exception e) {
            return "Error retrieving Redis info: " + e.getMessage();
        }
    }

    /**
     * ==================== 实用方法 ====================
     */

    /**
     * 将对象序列化为JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    /**
     * 将JSON字符串反序列化为对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类
     * @return 对象
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization error", e);
        }
    }

    /**
     * 生成缓存Key
     * 格式：cache:sass:{prefix}:{identifier}
     *
     * @param prefix     前缀（如user、product）
     * @param identifier 标识符（如ID）
     * @return 完整的缓存Key
     */
    public String generateKey(String prefix, Object identifier) {
        return String.format("cache:saas:%s:%s", prefix, identifier);
    }

    /**
     * 生成缓存Key（带分页）
     * 格式：cache:sass:{prefix}:{page}:{size}:{sort}
     *
     * @param prefix     前缀
     * @param page       页码
     * @param size       每页大小
     * @param sortBy     排序字段
     * @param sortDirection 排序方向
     * @return 完整的缓存Key
     */
    public String generateListKey(String prefix, int page, int size, String sortBy, String sortDirection) {
        return String.format("cache:saas:%s:list:%d:%d:%s:%s",
                prefix, page, size, sortBy, sortDirection);
    }

    /**
     * 生成分布式锁Key
     *
     * @param key 业务Key
     * @return 锁Key
     */
    public String generateLockKey(String key) {
        return String.format("lock:saas:%s", key);
    }
}
