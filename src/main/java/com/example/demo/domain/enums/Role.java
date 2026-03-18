package com.example.demo.domain.enums;

/**
 * 用户角色枚举
 */
public enum Role {
    /**
     * 普通用户
     */
    USER("USER", "普通用户"),

    /**
     * 管理员
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 超级管理员
     */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员");

    private final String code;
    private final String description;

    Role(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取对应的角色
     * @param code 角色代码
     * @return 对应的Role，如果未找到返回null
     */
    public static Role fromCode(String code) {
        for (Role role : Role.values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
