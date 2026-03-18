package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User实体测试类
 * 演示充血模型的使用
 */
class UserTest {

    @Test
    @DisplayName("测试创建用户成功")
    void testCreateUserSuccess() {
        // 当
        User user = new User("testuser", "password123", Role.USER);

        // 那么
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals(Role.USER, user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotEquals("password123", user.getPassword()); // 密码应该被加密
    }

    @Test
    @DisplayName("测试验证密码正确")
    void testVerifyPasswordCorrect() {
        // 假设
        User user = new User("testuser", "password123", Role.USER);

        // 当并那么
        assertTrue(user.verifyPassword("password123"));
    }

    @Test
    @DisplayName("测试验证密码错误")
    void testVerifyPasswordIncorrect() {
        // 假设
        User user = new User("testuser", "password123", Role.USER);

        // 当并那么
        assertFalse(user.verifyPassword("wrongpassword"));
    }

    @Test
    @DisplayName("测试更改密码成功")
    void testChangePasswordSuccess() {
        // 假设
        User user = new User("testuser", "oldpassword", Role.USER);

        // 当
        user.changePassword("oldpassword", "newpassword123");

        // 那么
        assertTrue(user.verifyPassword("newpassword123"));
        assertFalse(user.verifyPassword("oldpassword"));
    }

    @Test
    @DisplayName("测试更改密码失败（旧密码错误）")
    void testChangePasswordFail() {
        // 假设
        User user = new User("testuser", "oldpassword", Role.USER);

        // 当并那么
        assertThrows(IllegalArgumentException.class,
                () -> user.changePassword("wrongpassword", "newpassword123"));
    }

    @Test
    @DisplayName("测试设置无效的用户名")
    void testSetInvalidUsername() {
        // 当并那么
        User user = new User("testuser", "password", Role.USER);
        assertThrows(IllegalArgumentException.class, () -> user.setUsername(""));
        assertThrows(IllegalArgumentException.class, () -> user.setUsername("ab")); // 小于3位
        assertThrows(IllegalArgumentException.class, () -> user.setUsername("a".repeat(51))); // 大于50位
    }

    @Test
    @DisplayName("测试设置无效密码")
    void testSetInvalidPassword() {
        // 假设
        User user = new User("testuser", "password", Role.USER);

        // 当并那么
        assertThrows(IllegalArgumentException.class, () -> user.setPassword(""));
        assertThrows(IllegalArgumentException.class, () -> user.setPassword("12345")); // 少于6位
    }

    @Test
    @DisplayName("测试设置有效手机号")
    void testSetValidPhone() {
        // 假设
        User user = new User("testuser", "password", Role.USER);

        // 当
        user.setPhone("13800138000");

        // 那么
        assertEquals("13800138000", user.getPhone());
    }

    @Test
    @DisplayName("测试设置无效手机号")
    void testSetInvalidPhone() {
        // 假设
        User user = new User("testuser", "password", Role.USER);

        // 当并那么
        assertThrows(IllegalArgumentException.class, () -> user.setPhone("1380013800")); // 少了1位
        assertThrows(IllegalArgumentException.class, () -> user.setPhone("23800138000")); // 不是以1开头
        assertThrows(IllegalArgumentException.class, () -> user.setPhone("1380013800a")); // 包含字母
    }

    @Test
    @DisplayName("测试设置有效邮箱")
    void testSetValidEmail() {
        // 假设
        User user = new User("testuser", "password", Role.USER);

        // 当
        user.setEmail("test@example.com");

        // 那么
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    @DisplayName("测试设置无效邮箱")
    void testSetInvalidEmail() {
        // 假设
        User user = new User("testuser", "password", Role.USER);

        // 当并那么
        assertThrows(IllegalArgumentException.class, () -> user.setEmail("invalid-email"));
        assertThrows(IllegalArgumentException.class, () -> user.setEmail("@example.com"));
    }

    @Test
    @DisplayName("测试用户角色判断")
    void testUserRoleCheck() {
        // 当并那么
        User user = new User("testuser", "password", Role.USER);
        assertFalse(user.isAdmin());
        assertFalse(user.isSuperAdmin());

        User admin = new User("admin", "password", Role.ADMIN);
        assertTrue(admin.isAdmin());
        assertFalse(admin.isSuperAdmin());

        User superAdmin = new User("superadmin", "password", Role.SUPER_ADMIN);
        assertTrue(superAdmin.isAdmin());
        assertTrue(superAdmin.isSuperAdmin());
    }

    @Test
    @DisplayName("测试用户名匹配")
    void testUsernameMatching() {
        // 假设
        User user = new User("TestUser", "password", Role.USER);

        // 当并那么
        assertTrue(user.hasUsername("TestUser"));
        assertTrue(user.hasUsername("testuser")); // 忽略大小写
        assertTrue(user.hasUsername("  TestUser  ")); // 去除空格
        assertFalse(user.hasUsername("DifferentUser"));
    }

    @Test
    @DisplayName("测试更新用户资料")
    void testUpdateProfile() {
        // 假设
        User user = new User("testuser", "password", Role.USER);

        // 当
        user.updateProfile("13800138000", "newemail@example.com");

        // 那么
        assertEquals("13800138000", user.getPhone());
        assertEquals("newemail@example.com", user.getEmail());
    }
}
