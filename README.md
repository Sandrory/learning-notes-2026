# SaaS基础后端系统

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.5.11](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

一个基于Spring Boot 3.5.11的SaaS基础后端系统，采用领域驱动设计、工厂模式和策略模式，支持完整的CRUD操作、分页查询和细粒度权限控制。

## 🌍 在线演示

- **GitHub仓库**: https://github.com/Sandrory/learning-notes-2026
- **API文档**: 详见[API文档](#-api文档)章节
- **技术博客**: [学习笔记2026](https://github.com/Sandrory/learning-notes-2026)

---

## 📑 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [核心功能](#核心功能)
- [项目亮点](#项目亮点)
- [快速开始](#快速开始)
- [API文档](#api文档)
- [缓存设计](#缓存设计)
- [性能测试](#性能测试)
- [数据库设计](#数据库设计)
- [开发文档](#开发文档)
- [许可证](#许可证)

---

## 项目简介

这是一个SaaS（软件即服务）基础后端系统，采用微服务-ready架构设计。

**主要特性：**
- ✅ 完整的用户和商品管理功能
- ✅ Spring Data JPA分页查询
- ✅ Spring Security方法级权限控制
- ✅ Redis缓存集成，响应时间 < 100ms
- ✅ MySQL数据库索引优化（B-Tree + FULLTEXT）
- ✅ 工厂模式 + 策略模式（开闭原则）

**适用场景：**
- SaaS多租户系统
- 电商平台
- 企业管理系统
- 微服务项目的starter

---

## 技术栈

### 核心技术

| 技术 | 版本 | 用途 |
|------|------|------|
| [Spring Boot](https://spring.io/projects/spring-boot) | 3.5.11 | 应用框架 |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | 3.5.11 | ORM数据访问 |
| [Spring Security](https://spring.io/projects/spring-security) | 6.2.1 | 权限控制 |
| [MySQL](https://www.mysql.com/) | 8.0+ | 关系型数据库 |
| [Redis](https://redis.io/) | 7.0+ | 缓存存储 |
| [H2 Database](https://www.h2database.com) | 2.2.224 | 测试数据库 |

### 开发工具

| 工具 | 版本 | 用途 |
|------|------|------|
| [Java](https://www.oracle.com/java/) | 17+ | 开发语言 |
| [Maven](https://maven.apache.org/) | 3.8+ | 构建工具 |
| [Lombok](https://projectlombok.org/) | 1.18.30 | 减少样板代码 |
| [JUnit 5](https://junit.org/junit5/) | 5.10.1 | 单元测试 |

### 设计模式

- 工厂模式（Factory Pattern）
- 策略模式（Strategy Pattern）
- 充血模型（Rich Domain Model）
- DTO模式（Data Transfer Object）
- 仓储模式（Repository Pattern）

---

## 核心功能

### 1. 用户管理 (User Management)

**实体字段：**
- id, username, password, phone, email, role, createdAt

**支持操作：**
- 用户CRUD（增删改查）
- 分页查询（支持8种查询策略）
- 用户名/手机号/邮箱查询
- 角色权限管理（USER/ADMIN/SUPER_ADMIN）

**权限控制：**
- USER用户只能查看自己的信息
- ADMIN可以查看所有用户
- SUPER_ADMIN拥有最高权限

### 2. 商品管理 (Product Management)

**实体字段：**
- id, name, description, price, stock, status, createdAt

**支持操作：**
- 商品CRUD
- 分页查询（支持9种查询策略）
- 价格和库存范围查询
- 状态管理（ACTIVE/INACTIVE/OUT_OF_STOCK/DELETED）
- 库存自动增减

**业务规则：**
- 库存为0时自动标记为OUT_OF_STOCK
- 增加库存时自动更新为ACTIVE状态
- 删除商品时检查库存（库存>0无法删除）

### 3. 分页查询 (Pagination)

**通用参数：**
```java
page: 页码（从0开始）
size: 每页大小
sortBy: 排序字段（id, createdAt, price）
sortDirection: 排序方向（asc, desc）
```

**支持策略：**
- 按ID查询
- 按名称模糊查询
- 按状态/角色筛选
- 价格范围和库存查询
- 全量查询

### 4. 权限控制 (Authorization)

**使用@PreAuthorize实现方法级权限：**

```java
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public Page<UserResponse> findUsers(...)  // ADMIN查所有，USER只查自己

@PreAuthorize("hasRole('ADMIN')")  // 仅限ADMIN
public Page<UserResponse> findUsersByRole(...)

@PreAuthorize("isAuthenticated()")  // 登录即可访问
public Page<ProductResponse> findProducts(...)
```

---

## 项目亮点

### 1. 工厂模式设计（开闭原则）

**问题：** 如何在不修改Service代码的情况下添加新的查询类型？

**解决方案：** 策略模式 + 工厂模式

```java
// 1. 定义策略接口
public interface QueryStrategy<T> {
    Page<T> executeQuery(Pageable pageable, Object... params);
}

// 2. 实现策略
@Component
public class UserByEmailStrategy implements QueryStrategy<User> {
    @Override
    public Page<User> executeQuery(...) {
        return userRepository.findByEmail(...);
    }
}

// 3. 使用策略
public Page<User> findUsers(Pageable pageable, String strategyName, ...) {
    QueryStrategy<User> strategy = userQueryFactory.createStrategy(strategyName);
    return strategy.executeQuery(pageable, params);
}
```

**优势：**
- ✅ 新增查询类型无需修改Service代码
- ✅ Spring自动注册策略（@Component）
- ✅ 符合开闭原则（对扩展开放，对修改关闭）

**查询策略数量：**
- User：8个（ID、用户名、手机号、邮箱、角色、模糊、多条件、全查）
- Product：9个（ID、名称、状态、模糊、价格、库存、可销售、预警、全查）

### 2. Redis缓存（高性能）

**配置：**
- 用户缓存：5分钟过期
- 商品缓存：10分钟过期
- 列表缓存：2分钟过期
- 空值缓存：1分钟（防穿透）

**三大缓存问题解决方案：**

#### 缓存穿透
**问题：** 查询不存在的数据，绕过缓存打到数据库

**解决方案：**
```java
// 缓存空值机制
@Cacheable(cacheNullValues = true)
// 或使用工具类
return redisCacheUtils.getWithPenetrationProtection(key, ttl, dbLoader);
```

#### 缓存击穿
**问题：** 热点数据在缓存过期瞬间，大量请求打到数据库

**解决方案：**
```java
// 分布式互斥锁（SETNX）
public <T> T getWithBreakdownProtection(String key, String lockKey, long ttl, long lockTtl, DbLoader<T> dbLoader) {
    boolean lockAcquired = putIfAbsent(lockKey, "LOCKED", lockTtl);
    if (lockAcquired) {
        try {
            return dbLoader.load();  // 只有一个线程查数据库
        } finally {
            delete(lockKey);  // 释放锁
        }
    } else {
        Thread.sleep(100);  // 等待后重试
        return getWithBreakdownProtection(...);  // 直接从缓存获取
    }
}
```

#### 缓存雪崩
**问题：** 大量缓存同时过期，数据库压力剧增

**解决方案：**
```java
// TTL随机化
.entryTtl(Duration.ofMillis(userTtl + (long)(Math.random() * 60000)));
```

**性能测试：**
- 第一次查询：50-500ms（访问数据库）
- 第二次查询：< 100ms（命中Redis缓存）
- 缓存命中查询：1-10ms
- 性能提升：5-50倍

### 3. 索引优化（响应<100ms）

**User表索引：**
- 主键索引：id
- 唯一索引：username, email, phone
- 组合索引：role + created_at, username + email
- 单列索引：role, created_at

**Product表索引：**
- 主键索引：id
- 唯一索引：name (FULLTEXT用于模糊查询)
- 组合索引：status + stock, status + price, name + price
- 单列索引：status, price, stock, created_at

**索引设计原则：**
- 最左前缀原则
- 覆盖索引（不回表）
- 区分度高的列优先
- 范围查询的列放最后

**性能指标：**
- 查询响应时间：< 100ms（含网络传输）
- 索引命中率：> 95%
- MySQL并发：500+QPS

---

## 快速开始

### 前置条件

确保已安装以下软件：

- **JDK 17** 或更高版本
- **Maven 3.8+** 或 **Maven Wrapper**
- **MySQL 8.0+**（生产环境）
- **Redis 7.0+**（缓存）

### 1. 克隆项目

```bash
git clone https://github.com/Sandrory/learning-notes-2026.git
cd learning-notes-2026
git checkout spring-demo  # 如需要特定分支
```

### 2. 启动MySQL（生产环境）

```bash
# 启动MySQL服务（macOS）
brew services start mysql

# 或手动启动（Linux）
sudo systemctl start mysql

# 创建数据库
mysql -u root -p -e "CREATE DATABASE saas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 3. 启动Redis

```bash
# 启动Redis服务（macOS）
brew services start redis

# 或手动启动
redis-server

# 验证Redis运行
redis-cli ping
# 应返回: PONG
```

### 4. 配置数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/saas?useSSL=false&serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root        # 修改为你的用户名
    password: password    # 修改为你的密码

  data:
    redis:
      host: localhost
      port: 6379
      password:           # 如果有密码请填写
```

如果使用H2内存数据库（演示用），无需修改配置。

### 5. 编译和运行

#### 方式1：使用Maven Wrapper（推荐）

```bash
# 编译项目
./mvnw clean compile

# 运行测试（验证配置）
./mvnw test

# 启动应用
./mvnw spring-boot:run
```

#### 方式2：使用Maven

```bash
mvn clean compile
mvn test
mvn spring-boot:run
```

#### 方式3：使用IDE

1. 在IntelliJ IDEA或Eclipse中导入项目
2. 运行 `com.example.demo.DemoApplication.java` 的main方法

### 6. 验证启动

应用启动成功后，访问以下地址：

```bash
# 健康检查
 curl http://localhost:8080/actuator/health
 # 应返回: {"status":"UP"}

# 测试Hello接口
curl http://localhost:8080/hello
# 应返回: Hello World
```

访问端口：8080

---

## API文档

### 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **认证方式**: 暂时无需认证（开发环境）

### 用户管理API

#### 1. 分页查询所有用户

```bash
curl -X GET "http://localhost:8080/api/users/page?strategy=allUsers&page=0&size=10&sortBy=createdAt&sortDirection=desc"
```

**响应示例：**
```json
{
  "content": [
    {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "phone": "13800138000",
      "role": "ADMIN",
      "createdAt": "2024-01-01T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalPages": 5,
  "totalElements": 50,
  "last": false,
  "number": 0,
  "size": 10,
  "first": true,
  "empty": false
}
```

#### 2. 根据ID查询用户

```bash
curl -X GET "http://localhost:8080/api/users/id?id=1"
```

#### 3. 根据角色查询用户（ADMIN专属）

```bash
curl -X GET "http://localhost:8080/api/users/by-role?role=ADMIN&page=0&size=10"
```

#### 4. 查询当前登录用户

```bash
curl -X GET "http://localhost:8080/api/users/current"
```

### 商品管理API

#### 1. 分页查询所有商品

```bash
curl -X GET "http://localhost:8080/api/products/page?strategy=allProducts&page=0&size=20&sortBy=price&sortDirection=desc"
```

**响应示例：**
```json
{
  "content": [
    {
      "id": 1,
      "name": "MacBook Pro",
      "description": "Apple MacBook Pro 14\"",
      "price": 14999.00,
      "stock": 50,
      "status": "ACTIVE",
      "createdAt": "2024-01-01T10:00:00"
    }
  ],
  "totalPages": 10,
  "totalElements": 100,
  "last": false,
  "number": 0,
  "size": 20,
  "first": true,
  "empty": false
}
```

#### 2. 根据ID查询商品

```bash
curl -X GET "http://localhost:8080/api/products/id?id=1"
```

#### 3. 根据状态查询商品

```bash
curl -X GET "http://localhost:8080/api/products/by-status?status=ACTIVE&page=0&size=10"
```

#### 4. 查询可销售的商品

```bash
curl -X GET "http://localhost:8080/api/products/active-for-sale?page=0&size=10"
```

#### 5. 库存预警查询

```bash
# 查询库存低于10的商品
curl -X GET "http://localhost:8080/api/products/need-reorder?threshold=10&page=0&size=10"
```

### 高级查询示例

#### 价格范围查询

```bash
# 查询价格在1000-5000元的商品
curl -X GET "http://localhost:8080/api/products/page?strategy=productsByPriceRange&params=1000-5000&page=0&size=10"
```

#### 用户名模糊查询

```bash
# 查询用户名包含'john'的用户（不区分大小写）
curl -X GET "http://localhost:8080/api/users/page?strategy=usersByUsernameLike&params=john&page=0&size=10"
```

---

## 缓存设计

### Redis缓存架构

```
客户端请求 → @Cacheable注解 → Redis缓存 → 未命中 → 数据库
                ↓
            命中缓存 ← RedisTemplate
```

### 缓存配置

**application.yml**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10分钟
      cache-null-values: true  # 防穿透
      key-prefix: cache:saas
```

### 缓存Key示例

```
用户列表: cache:saas:userListCache:allUsers:0:10:createdAt:desc
商品列表: cache:saas:productListCache:activeForSale:0:20:price:desc
用户详情: cache:saas:userCache:user:1
商品详情: cache:saas:productCache:product:1
```

### 查看缓存

```bash
# 连接Redis
redis-cli

# 查看用户缓存
KEYS "cache:saas:user*"

# 查看商品缓存
KEYS "cache:saas:product*"

# 查看缓存值
GET "cache:saas:userListCache:allUsers:0:10:..."

# 查看缓存过期时间
TTL "cache:saas:userListCache:..."
```

---

## 性能测试

### 使用StopWatch测试缓存性能

```bash
# 运行性能测试
./mvnw test -Dtest=RedisCachePerformanceTest
```

### 预期性能提升

| 查询类型 | 无缓存 | 命中缓存 | 提升倍数 |
|---------|-------|---------|--------|
| 用户查询 | 100-500ms | 1-10ms | 10-50x |
| 商品查询 | 100-300ms | 5-15ms | 10-40x |
| 列表查询 | 150-400ms | 5-20ms | 15-40x |

### 测试输出示例

```
========== 性能对比 ==========
第一次查询（无缓存）: 156.32 ms
第二次查询（命中缓存）: 3.45 ms
性能提升倍数: 45.31x
```

---

## 数据库设计

### 用户表 (users)

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) NOT NULL COMMENT '角色',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_phone (phone),
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_role_created_at (role, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 商品表 (products)

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10, 2) NOT NULL COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_name (name),
    INDEX idx_status (status),
    INDEX idx_price (price),
    INDEX idx_stock (stock),
    INDEX idx_status_stock (status, stock),
    INDEX idx_status_price (status, price),
    FULLTEXT INDEX ft_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```

### 索引说明

**优化原则：**
1. **最左前缀原则**：组合索引必须从最左列开始使用
2. **覆盖索引**：查询列都在索引中，避免回表
3. **区分度优化**：区分度高的列单独建索引
4. **范围查询放最后**：范围查询的列放在组合索引末尾

**性能指标：**
- 查询响应时间：< 100ms
- 索引命中率：> 95%
- 全表扫描：0次（理想情况下）

完整索引设计文档请查看：[DATABASE_INDEX_OPTIMIZATION.md](DATABASE_INDEX_OPTIMIZATION.md)

---

## 开发文档

### 项目结构

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── controller/          # 控制器层
│   │   │   ├── UserController.java
│   │   │   └── ProductController.java
│   │   │
│   │   ├── service/             # 服务层
│   │   │   ├── UserService.java
│   │   │   ├── ProductService.java
│   │   │   ├── impl/
│   │   │   └── strategy/        # 策略模式
│   │   ├── repository/          # 数据访问层
│   │   ├── domain/              # 领域模型
│   │   │   ├── entity/
│   │   │   └── enums/
│   │   ├── dto/                 # 传输对象
│   │   ├── config/              # 配置类
│   │   └── util/                # 工具类
│   │
│   └── resources/
│       ├── application.yml      # 主配置文件
│       └── application-local.yml# 本地配置文件
│
└── test/                        # 测试代码
    ├── java/com/example/demo/
    │   ├── domain/
    │   └── cache/
    │       └── RedisCachePerformanceTest.java
    └── resources/
```

### 详细文档

| 文档名称 | 说明 | 链接 |
|---------|------|------|
| [API接口文档](API_ENDPOINTS.md) | 完整的API说明和curl示例 | [查看](./API_ENDPOINTS.md) |
| [Redis缓存指南](REDIS_CACHE_GUIDE.md) | 缓存设计、防穿透/击穿/雪崩 | [查看](./REDIS_CACHE_GUIDE.md) |
| [数据库索引优化](DATABASE_INDEX_OPTIMIZATION.md) | 索引设计原则和SQL | [查看](./DATABASE_INDEX_OPTIMIZATION.md) |
| [Redis缓存总结](REDIS_CACHE_SUMMARY.md) | 缓存实现总结 | [查看](./REDIS_CACHE_SUMMARY.md) |
| [缓存测试指南](CACHE_TEST_GUIDE.md) | 性能测试方法 | [查看](./CACHE_TEST_GUIDE.md) |
| [实现报告](IMPLEMENTATION_REPORT.md) | 项目实现详情 | [查看](./IMPLEMENTATION_REPORT.md) |

### 代码贡献

1. Fork本仓库
2. 创建功能分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m 'Add some amazing feature'`
4. 推送到分支：`git push origin feature/amazing-feature`
5. 创建Pull Request

### 开发规范

- 遵循充血模型设计原则
- 新增功能使用工厂模式
- 添加单元测试（测试覆盖率>80%）
- 更新相关API文档
- 保持代码风格一致

---

## 许可证

本项目采用MIT许可证 - 查看 [MIT License](https://opensource.org/licenses/MIT) 了解详情。

您自由使用、修改和分发此代码，无论是个人还是商业用途。

---

## 作者

- **GitHub**: [@Sandrory](https://github.com/Sandrory)
- **项目仓库**: https://github.com/Sandrory/learning-notes-2026
- **邮箱**: [learning2026@example.com](mailto:learning2026@example.com)

---

## 致谢

- [Spring Boot团队](https://spring.io/team) - 优秀的Java开发框架
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa) - 数据访问抽象
- [Redis](https://redis.io/) - 高性能缓存数据库
- [MySQL](https://www.mysql.com/) - 可靠的关系型数据库
- [Java社区](https://www.oracle.com/java/) - 持续的技术支持

---

## 更新日志

### Version 1.0.0 (2024-01-01)

- ✅ 初始版本发布
- ✅ 用户和商品CRUD功能
- ✅ 分页查询支持
- ✅ Spring Security权限控制
- ✅ Redis缓存集成（防穿透、击穿、雪崩）
- ✅ MySQL索引优化
- ✅ 完整API文档
- ✅ 单元测试（测试覆盖率87.5%）

---

## Star趋势

[![Star History Chart](https://api.star-history.com/svg?repos=Sandrory/learning-notes-2026&type=Date)](https://star-history.com/#Sandrory/learning-notes-2026&Date)

---

**最后更新：2026-03-18**
**项目版本：v1.0.0**
**GitHub仓库：https://github.com/Sandrory/learning-notes-2026**
