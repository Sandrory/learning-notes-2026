package com.example.demo.service.strategy.factory;

import com.example.demo.service.strategy.QueryStrategy;

/**
 * 查询策略工厂接口
 * 使用工厂模式创建查询策略实例
 * 支持开闭原则：新增查询类型无需修改工厂
 */
public interface QueryStrategyFactory<T> {

    /**
     * 根据策略名称创建查询策略
     * @param strategyName 策略名称
     * @return 对应的查询策略
     * @throws IllegalArgumentException 如果策略不存在
     */
    QueryStrategy<T> createStrategy(String strategyName);

    /**
     * 检查策略是否存在
     * @param strategyName 策略名称
     * @return 如果策略存在返回true，否则返回false
     */
    boolean containsStrategy(String strategyName);
}