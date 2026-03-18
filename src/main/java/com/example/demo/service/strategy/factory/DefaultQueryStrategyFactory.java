package com.example.demo.service.strategy.factory;

import com.example.demo.service.strategy.QueryStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认查询策略工厂实现
 * 线程安全的策略工厂，维护策略注册表
 *
 * @param <T> 策略处理的数据类型
 */
public class DefaultQueryStrategyFactory<T> implements QueryStrategyFactory<T> {

    /**
     * 策略注册表，存储策略名称和策略实例的映射
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, QueryStrategy<T>> strategyRegistry = new ConcurrentHashMap<>();

    /**
     * 注册查询策略
     * @param strategyName 策略名称
     * @param strategy 策略实例
     * @throws IllegalArgumentException 如果策略名称为空或策略已存在
     */
    public void registerStrategy(String strategyName, QueryStrategy<T> strategy) {
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new IllegalArgumentException("策略名称不能为空");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("策略实例不能为空");
        }
        if (strategyRegistry.containsKey(strategyName)) {
            throw new IllegalArgumentException("策略已存在: " + strategyName);
        }
        strategyRegistry.put(strategyName.trim(), strategy);
    }

    /**
     * 取消注册查询策略
     * @param strategyName 策略名称
     */
    public void unregisterStrategy(String strategyName) {
        strategyRegistry.remove(strategyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryStrategy<T> createStrategy(String strategyName) {
        if (strategyName == null) {
            throw new IllegalArgumentException("策略名称不能为空");
        }
        QueryStrategy<T> strategy = strategyRegistry.get(strategyName.trim());
        if (strategy == null) {
            throw new IllegalArgumentException("未找到策略: " + strategyName);
        }
        return strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsStrategy(String strategyName) {
        return strategyName != null && strategyRegistry.containsKey(strategyName.trim());
    }

    /**
     * 获取已注册的策略数量
     * @return 策略数量
     */
    public int getStrategyCount() {
        return strategyRegistry.size();
    }

    /**
     * 清空所有注册的策略
     */
    public void clearStrategies() {
        strategyRegistry.clear();
    }
}