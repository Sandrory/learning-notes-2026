package com.example.demo.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis配置类
 * 配置RedisTemplate、CacheManager和缓存过期时间
 */
@Configuration
@EnableCaching  // 启用Spring缓存
public class RedisConfig implements CachingConfigurer {

    @Value("${app.cache.user-ttl:300000}")
    private long userTtl;

    @Value("${app.cache.product-ttl:600000}")
    private long productTtl;

    @Value("${app.cache.list-ttl:120000}")
    private long listTtl;

    @Value("${app.cache.null-ttl:60000}")
    private long nullTtl;

    /**
     * 配置RedisTemplate
     * 用于手动操作Redis
     */
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
        // 注册Java时间模块，支持LocalDateTime等
        objectMapper.registerModule(new JavaTimeModule());
        serializer.setObjectMapper(objectMapper);

        // 设置Key和Value的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }

    /**
     * 配置CacheManager
     * 管理Spring缓存注解（@Cacheable、@CacheEvict等）
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(productTtl))  // 默认10分钟过期
                .disableCachingNullValues()  // 不缓存null值（防穿透）
                .prefixCacheNameWith("cache:saas:")  // 缓存前缀
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(createJacksonRedisSerializer())
                );

        // 自定义缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 用户缓存配置（5分钟）
        cacheConfigurations.put("userCache",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMillis(userTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(createJacksonRedisSerializer())
                        )
        );

        // 商品缓存配置（10分钟）
        cacheConfigurations.put("productCache",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMillis(productTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(createJacksonRedisSerializer())
                        )
        );

        // 用户列表缓存配置（2分钟）
        cacheConfigurations.put("userListCache",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMillis(listTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(createJacksonRedisSerializer())
                        )
        );

        // 商品列表缓存配置（2分钟）
        cacheConfigurations.put("productListCache",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMillis(listTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(createJacksonRedisSerializer())
                        )
        );

        // 允许缓存null值的配置（1分钟，用于防穿透）
        cacheConfigurations.put("nullValueCache",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMillis(nullTtl))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(createJacksonRedisSerializer())
                        )
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 创建Jackson序列化器
     */
    private Jackson2JsonRedisSerializer<Object> createJacksonRedisSerializer() {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        objectMapper.registerModule(new JavaTimeModule());
        serializer.setObjectMapper(objectMapper);
        return serializer;
    }

    /**
     * 配置StringRedisTemplate
     * 专门用于操作字符串类型的数据
     */
    @Bean
    public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        return new org.springframework.data.redis.core.StringRedisTemplate(connectionFactory);
    }

    /**
     * 配置RedisCacheWriter（可选，用于更高级的缓存操作）
     */
    @Bean
    public org.springframework.data.redis.cache.RedisCacheWriter redisCacheWriter(
            RedisConnectionFactory connectionFactory) {
        return org.springframework.data.redis.cache.RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
    }
}
