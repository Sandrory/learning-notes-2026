package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户实体类
 * 使用充血模型，封装领域行为
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名，唯一且不能为空
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码，加密存储
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 邮箱，唯一
     */
    @Column(name = "email", unique = true, length = 100)
    private String email;

    /**
     * 用户角色
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 盐值，用于密码加密
     */
    private static final String SALT = "ClaudeCode2024";

    /**
     * 加密密码（使用SHA-256）
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    private String encryptPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = rawPassword + SALT;
            byte[] hash = digest.digest(saltedPassword.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 手机号正则表达式
     */
    @Transient
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 邮箱正则表达式
     */
    @Transient
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * 空构造函数，JPA需要
     */
    protected User() {
    }

    /**
     * 构造函数
     * @param username 用户名
     * @param rawPassword 原始密码
     * @param role 角色
     */
    public User(String username, String rawPassword, Role role) {
        setUsername(username);
        this.role = role;
        setPassword(rawPassword);
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Getter方法 ====================

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ==================== Setter方法 ====================

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("用户名长度必须在3-50个字符之间");
        }
        this.username = username.trim();
    }

    /**
     * 设置密码（自动加密）
     * @param rawPassword 原始密码
     */
    public void setPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }
        this.password = encryptPassword(rawPassword);
    }

    public void setPhone(String phone) {
        if (phone != null && !phone.isEmpty()) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
        }
        this.phone = phone;
    }

    public void setEmail(String email) {
        if (email != null && !email.isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("邮箱格式不正确");
            }
        }
        this.email = email;
    }

    public void setRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("角色不能为空");
        }
        this.role = role;
    }

    // ==================== 领域行为方法（充血模型） ====================

    /**
     * 验证密码是否正确
     * @param rawPassword 原始密码
     * @return 如果密码正确返回true，否则返回false
     */
    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        return encryptPassword(rawPassword).equals(this.password);
    }

    /**
     * 更改密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void changePassword(String oldPassword, String newPassword) {
        if (!verifyPassword(oldPassword)) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        setPassword(newPassword);
    }

    /**
     * 判断用户是否为管理员
     * @return 如果是管理员返回true，否则返回false
     */
    public boolean isAdmin() {
        return this.role == Role.ADMIN || this.role == Role.SUPER_ADMIN;
    }

    /**
     * 判断用户是否为超级管理员
     * @return 如果是超级管理员返回true，否则返回false
     */
    public boolean isSuperAdmin() {
        return this.role == Role.SUPER_ADMIN;
    }

    /**
     * 检查用户名是否与指定用户名匹配（忽略大小写）
     * @param username 要检查的用户名
     * @return 如果匹配返回true，否则返回false
     */
    public boolean hasUsername(String username) {
        if (username == null || this.username == null) {
            return false;
        }
        return this.username.equalsIgnoreCase(username.trim());
    }

    /**
     * 更新用户信息
     * @param phone 新手机号
     * @param email 新邮箱
     */
    public void updateProfile(String phone, String email) {
        setPhone(phone);
        setEmail(email);
    }

    // ==================== 重写Object方法 ====================

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
