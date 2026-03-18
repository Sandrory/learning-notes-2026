package com.example.demo.service.strategy;

/**
 * 用户查询策略常量类
 * 定义了所有可用的用户查询策略名称
 */
public class UserConstants {

    private UserConstants() {
        // 私有构造函数，防止实例化
    }

    /**
     * 根据ID查询策略
     */
    public static final String USER_QUERY_BY_ID = "userById";

    /**
     * 根据用户名查询策略
     */
    public static final String USER_QUERY_BY_USERNAME = "userByUsername";

    /**
     * 根据邮箱查询策略
     */
    public static final String USER_QUERY_BY_EMAIL = "userByEmail";

    /**
     * 根据手机号查询策略
     */
    public static final String USER_QUERY_BY_PHONE = "userByPhone";

    /**
     * 根据角色查询策略
     */
    public static final String USER_QUERY_BY_ROLE = "usersByRole";

    /**
     * 根据用户名模糊查询策略
     */
    public static final String USER_QUERY_BY_USERNAME_LIKE = "usersByUsernameLike";

    /**
     * 根据用户名、邮箱或手机号查询策略
     */
    public static final String USER_QUERY_BY_USERNAME_OR_EMAIL_OR_PHONE = "usersByUsernameOrEmailOrPhone";

    /**
     * 查询所有用户策略
     */
    public static final String USER_QUERY_ALL = "allUsers";
}
