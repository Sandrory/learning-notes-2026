package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置类
 * 启用方法级权限控制（@PreAuthorize）
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 密码编码器配置
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security过滤链配置
     * 由于是演示目的，暂时允许所有请求
     * 实际项目中应该配置JWT或Session认证
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 允许所有请求（演示用）
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable()); // 禁用CSRF（演示用）

        return http.build();
    }
}
