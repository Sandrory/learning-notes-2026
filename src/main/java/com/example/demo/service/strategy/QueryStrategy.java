package com.example.demo.service.strategy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 查询策略接口
 * 定义了查询的基本操作
 * 支持分页和不分页两种查询模式
 */
public interface QueryStrategy<T> {

    /**
     * 执行不分页查询
     * @param params 查询参数（可变参数）
     * @return 查询结果列表
     */
    @SuppressWarnings("unchecked")
    List<T> executeQuery(Object... params);

    /**
     * 执行分页查询
     * @param params 查询参数
     * @param pageable 分页参数
     * @return 分页查询结果
     */
    @SuppressWarnings("unchecked")
    Page<T> executeQuery(Pageable pageable, Object... params);

    /**
     * 获取策略名称（用于工厂识别）
     * @return 策略名称
     */
    String getStrategyName();
}
