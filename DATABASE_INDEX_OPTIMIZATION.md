# 数据库索引优化指南

## 概述

本文档为SaaS基础系统的User和Product表提供索引优化建议，包括：

1. User表：用户名查询、手机号查询优化
2. Product表：名称模糊查询、价格范围查询优化
3. SQL建表语句（包含完整索引）
4. 索引设计原则详解

---

## 一、User表索引优化

### 查询场景分析

#### 场景1：根据用户名精确查询

```java
@Query("SELECT u FROM User u WHERE u.username = :username")
Optional<User> findByUsername(String username);
```

**SQL:**
```sql
SELECT * FROM users WHERE username = 'john_doe';
```

**索引优化建议：**
- 为 `username` 字段创建基于B-Tree的索引（主查询）
- 需要考虑大小写不敏感的查询（LOWER函数）
- 为邮箱和手机号创建组合索引（备用查询路径）

#### 场景2：根据手机号精确查询

```java
@Query("SELECT u FROM User u WHERE u.phone = :phone")
Optional<User> findByPhone(String phone);
```

**SQL:**
```sql
SELECT * FROM users WHERE phone = '13800138000';
```

**索引优化建议：**
- 为 `phone` 字段创建基于B-Tree的索引
- `phone` 字段长度固定（11位手机号），适合索引

#### 场景3：用户名模糊查询（不区分大小写）

```java
@Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))")
List<User> findByUsernameContainingIgnoreCase(String username);
```

**SQL:**
```sql
SELECT * FROM users WHERE LOWER(username) LIKE '%john%';
```

**索引优化建议：**
- LIKE '%xxx%' 无法使用B-Tree索引（左前缀匹配失效）
- 考虑使用全文索引（Full-Text Index）或Trigram索引（PostgreSQL）
- 对于MySQL，可以使用 `FULLTEXT` 索引

### User表优化后的SQL建表语句

```sql
-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) NOT NULL COMMENT '角色（USER/ADMIN/SUPER_ADMIN）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 唯一索引
    UNIQUE KEY uk_username (username) COMMENT '用户名唯一索引',
    UNIQUE KEY uk_email (email) COMMENT '邮箱唯一索引',
    UNIQUE KEY uk_phone (phone) COMMENT '手机号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建索引（在CREATE TABLE后）

-- 1. 用户名索引（支持精确查询和右模糊查询）
CREATE INDEX idx_username ON users(username) COMMENT '用户名查询索引';

-- 2. 手机号索引（精确查询）
CREATE INDEX idx_phone ON users(phone) COMMENT '手机号查询索引';

-- 3. 邮箱索引（精确查询）
CREATE INDEX idx_email ON users(email) COMMENT '邮箱查询索引';

-- 4. 角色索引（用于按角色分组查询）
CREATE INDEX idx_role ON users(role) COMMENT '角色查询索引';

-- 5. 创建时间索引（用于时间范围查询）
CREATE INDEX idx_created_at ON users(created_at) COMMENT '创建时间索引';

-- 6. 组合索引：角色 + 创建时间（支持按角色分页查询）
CREATE INDEX idx_role_created_at ON users(role, created_at) COMMENT '角色+创建时间组合索引';

-- 7. 组合索引：用户名 + 邮箱（备用多条件查询）
CREATE INDEX idx_username_email ON users(username, email) COMMENT '用户名+邮箱组合索引';
```

### User表索引详解

| 索引名称 | 索引类型 | 索引字段 | 用途 | 索引设计原则 |
|---------|---------|---------|------|------------|
| PRIMARY | 主键索引 | `id` | 主键查询 | 聚簇索引 |
| uk_username | 唯一索引 | `username` | 用户名唯一约束 | 业务唯一性 |
| uk_email | 唯一索引 | `email` | 邮箱唯一约束 | 业务唯一性 |
| uk_phone | 唯一索引 | `phone` | 手机号唯一约束 | 业务唯一性 |
| idx_username | B-Tree | `username` | 用户名查询 | 精确查询 |
| idx_phone | B-Tree | `phone` | 手机号查询 | 精确查询 |
| idx_email | B-Tree | `email` | 邮箱查询 | 精确查询 |
| idx_role | B-Tree | `role` | 按角色查询 | 分组查询 |
| idx_created_at | B-Tree | `created_at` | 时间范围查询 | 范围查询 |
| idx_role_created_at | 组合索引 | `role`, `created_at` | 按角色分页查询 | 最左前缀 |
| idx_username_email | 组合索引 | `username`, `email` | 用户名+邮箱查询 | 最左前缀 |

---

## 二、Product表索引优化

### 查询场景分析

#### 场景1：商品名称模糊查询

```java
@Query("SELECT p FROM Product p WHERE p.name LIKE %:name%")
List<Product> findByNameContainingIgnoreCase(String name);
```

**SQL:**
```sql
SELECT * FROM products WHERE name LIKE '%MacBook%';
```

**索引优化建议：**
- `LIKE '%xxx%'` 无法使用B-Tree索引（最左匹配失效）
- 考虑以下方案：
  - **方案1：** 使用全文索引（FULLTEXT）- MySQL 5.6+
  - **方案2：** 使用Elasticsearch等搜索引擎
  - **方案3：** 限制模糊查询在右端（`LIKE 'xxx%'` 可以使用索引）

#### 场景2：价格范围查询

```java
@Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
```

**SQL:**
```sql
SELECT * FROM products WHERE price BETWEEN 1000.00 AND 5000.00;
```

**索引优化建议：**
- 为 `price` 字段创建B-Tree索引
- 范围查询适合使用B-Tree索引（有序）
- 可以考虑组合索引（状态 + 价格）

#### 场景3：状态查询 + 价格排序

```java
Page<Product> findByStatus(ProductStatus status, Pageable pageable);
```

**SQL:**
```sql
SELECT * FROM products WHERE status = 'ACTIVE'
ORDER BY price DESC LIMIT 0, 20;
```

**索引优化建议：**
- 创建组合索引 `(status, price)` 避免filesort
- `status` 用于WHERE条件，`price` 用于ORDER BY
- 符合最左前缀原则

#### 场景4：可销售商品查询（状态 + 库存）

```java
@Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.stock > 0")
List<Product> findActiveForSale();
```

**SQL:**
```sql
SELECT * FROM products WHERE status = 'ACTIVE' AND stock > 0;
```

**索引优化建议：**
- 创建组合索引 `(status, stock)`
- 两个字段都用于WHERE条件
- 复合索引可以高效支持多条件查询

### Product表优化后的SQL建表语句

```sql
-- 商品表
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10, 2) NOT NULL COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/INACTIVE/OUT_OF_STOCK/DELETED）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_name (name) COMMENT '商品名称索引',
    INDEX idx_status (status) COMMENT '状态索引',
    INDEX idx_price (price) COMMENT '价格索引',
    INDEX idx_stock (stock) COMMENT '库存索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引',

    -- 组合索引：状态 + 库存（用于可销售商品查询）
    INDEX idx_status_stock (status, stock) COMMENT '状态+库存组合索引',

    -- 组合索引：状态 + 价格（用于按状态+价格排序）
    INDEX idx_status_price (status, price) COMMENT '状态+价格组合索引',

    -- 组合索引：名称 + 价格（用于名称+价格范围查询）
    INDEX idx_name_price (name, price) COMMENT '名称+价格组合索引',

    -- 全文索引：商品名称（用于模糊查询）
    FULLTEXT INDEX ft_name (name) COMMENT '商品名称全文索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 额外创建全文索引（如果版本支持）
ALTER TABLE products ADD FULLTEXT INDEX ft_description (description) COMMENT '商品描述全文索引';
```

### Product表索引详解

| 索引名称 | 索引类型 | 索引字段 | 用途 | 索引设计原则 |
|---------|---------|---------|------|------------|
| PRIMARY | 主键索引 | `id` | 主键查询 | 聚簇索引 |
| idx_name | B-Tree | `name` | 商品名称查询 | 精确查询、右模糊 |
| idx_status | B-Tree | `status` | 按状态查询 | 枚举字段 |
| idx_price | B-Tree | `price` | 价格范围查询 | 范围查询 |
| idx_stock | B-Tree | `stock` | 库存查询 | 范围查询 |
| idx_created_at | B-Tree | `created_at` | 时间范围查询 | 范围查询 |
| idx_status_stock | 组合索引 | `status`, `stock` | 可销售商品查询 | 多条件查询 |
| idx_status_price | 组合索引 | `status`, `price` | 按状态+价格排序 | 避免filesort |
| idx_name_price | 组合索引 | `name`, `price` | 名称+价格查询 | 最左前缀 |
| ft_name | FULLTEXT | `name` | 名称模糊查询 | 全文搜索 |
| ft_description | FULLTEXT | `description` | 描述模糊查询 | 长文本搜索 |

---

## 三、索引设计原则详解

### 原则1：最左前缀原则（Left-First-Prefix）

**定义：**

组合索引（复合索引）中，查询条件必须从索引的最左边开始，才能使用索引。

**示例：**

```sql
-- 组合索引：(status, price, created_at)
CREATE INDEX idx_status_price_created_at ON products(status, price, created_at);

-- ✅ 可以使用索引
SELECT * FROM products WHERE status = 'ACTIVE';
SELECT * FROM products WHERE status = 'ACTIVE' AND price = 1000;
SELECT * FROM products WHERE status = 'ACTIVE' AND price = 1000 AND created_at > '2024-01-01';

-- ❌ 不能使用索引（缺少最左字段）
SELECT * FROM products WHERE price = 1000;
SELECT * FROM products WHERE price = 1000 AND created_at > '2024-01-01';

-- ❌ 不能使用索引（跳过中间字段）
SELECT * FROM products WHERE status = 'ACTIVE' AND created_at > '2024-01-01';
```

**应用：**
- 在我们的设计中：
  - `idx_status_price` 支持 `WHERE status='ACTIVE' ORDER BY price`
  - 不支持单独 `ORDER BY price`（缺少status条件）

### 原则2：覆盖索引（Covering Index）

**定义：**

索引包含了查询所需的所有字段，无需回表查询数据行。

**示例：**

```sql
-- 组合索引：(id, username, email)
CREATE INDEX idx_id_username_email ON users(id, username, email);

-- ✅ 覆盖索引查询（不需要访问数据行）
SELECT id, username, email FROM users WHERE id = 1;

-- ❌ 非覆盖索引（需要回表查询phone）
SELECT id, username, phone FROM users WHERE id = 1;
```

**优点：**
- 减少IO操作，大幅提升性能
- 避免回表查询

**应用：**
- `idx_status_price`：查询 `WHERE status='ACTIVE' ORDER BY price` 时，
  如果SELECT只包含status和price，则构成覆盖索引

### 原则3：区分度高的列优先

**定义：**

将区分度（Cardinality）高的列放在组合索引的前面。

**区分度计算：**
```
区分度 = 不同值的数量 / 总行数
```

**示例：**

```sql
-- users表有10000条记录
-- status字段只有3个值（区分度低：3/10000 = 0.03%）
-- username字段有10000个不同值（区分度高：100%）

-- ❌ 错误的索引顺序
CREATE INDEX idx_status_username ON users(status, username);

-- ✅ 正确的索引顺序
CREATE INDEX idx_username_status ON users(username, status);
```

**应用：**
- 在项目中：`username`、`email`、`phone` 都是高区分度字段，适合创建单独索引
- `status` 是低区分度字段，应该放在组合索引的后面

### 原则4：避免在索引列上使用函数

**定义：**

在WHERE条件中对索引列使用函数会导致索引失效。

**示例：**

```sql
-- 假设有索引 idx_created_at (created_at)

-- ❌ 索引失效（使用函数）
SELECT * FROM users WHERE DATE(created_at) = '2024-01-01';

-- ✅ 可以使用索引（不使用函数）
SELECT * FROM users WHERE created_at >= '2024-01-01' AND created_at < '2024-01-02';

-- ❌ 索引失效（使用函数）
SELECT * FROM users WHERE LOWER(username) = 'john_doe';

-- ✅ 可以考虑使用函数索引（MySQL 8.0+）
CREATE INDEX idx_lower_username ON users((LOWER(username)));
```

**应用：**
- 在项目中：模糊查询使用了 `LOWER(username)`，可以考虑创建函数索引
- 或者在应用层统一存储小写格式

### 原则5：最左匹配 vs 跳跃匹配

**定义：**

组合索引只能使用最左连续的列。

**示例：**

```sql
-- 组合索引：(a, b, c)
CREATE INDEX idx_a_b_c ON table_name(a, b, c);

-- ✅ 完整匹配
WHERE a = 1 AND b = 2 AND c = 3

-- ✅ 最左前缀匹配
WHERE a = 1
WHERE a = 1 AND b = 2

-- ❌ 跳跃匹配（跳过b）
WHERE a = 1 AND c = 3  -- 只能使用a的索引部分

-- ❌ 非最左匹配
WHERE b = 2 AND c = 3  -- 完全不能使用索引
```

**应用：**
- 在我们的组合索引设计中，必须确保查询条件包含最左列
- `idx_status_price` 必须包含status条件才能使用price排序

### 原则6：范围查询的列放最后

**定义：**

组合索引中，范围查询（>、<、BETWEEN、LIKE）的列应该放在最后。

**原因：**

范围查询后的列无法使用索引。

**示例：**

```sql
-- ❌ 错误的索引顺序（范围查询在中间）
CREATE INDEX idx_price_status ON products(price, status);

-- 查询：WHERE price BETWEEN 100 AND 500 AND status = 'ACTIVE'
-- 只能使用price部分索引，status无法使用

-- ✅ 正确的索引顺序（范围查询在最后）
CREATE INDEX idx_status_price ON products(status, price);

-- 查询：WHERE status = 'ACTIVE' AND price BETWEEN 100 AND 500
-- 两个条件都可以使用索引
```

**应用：**
- `idx_status_price`：status（等值查询）在前，price（范围查询）在后
- `idx_status_stock`：status（等值查询）在前，stock（范围查询）在后

---

## 四、模糊查询索引优化方案

### 问题：LIKE '%xxx%' 无法使用B-Tree索引

```sql
SELECT * FROM products WHERE name LIKE '%MacBook%';
```

### 解决方案1：全文索引（MySQL 5.6+）

```sql
-- 创建全文索引
ALTER TABLE products ADD FULLTEXT INDEX ft_name (name);

-- 使用全文搜索
SELECT * FROM products WHERE MATCH(name) AGAINST('MacBook' IN BOOLEAN MODE);

-- 查询模式：
-- +MacBook: 必须包含MacBook
-- +MacBook -Pro: 必须包含MacBook且不包含Pro
-- MacBook*: 以MacBook开头的词
```

**优点：**
- 专门为文本搜索优化
- 支持复杂的搜索语法

**缺点：**
- MySQL全文索引性能不如Elasticsearch
- 不支持中文分词（需要插件）

### 解决方案2：Elasticsearch集成

**架构：**
```
应用 -> MySQL（主存储）
    -> Elasticsearch（搜索）
```

**同步方案：**
- 使用Canal监听MySQL binlog，同步到ES
- 应用层双写（写入MySQL同时写入ES）

### 解决方案3：限制模糊查询位置

```sql
-- 将查询改为右模糊（可以使用索引）
-- 应用层限制用户输入
SELECT * FROM products WHERE name LIKE 'MacBook%';  -- ✅ 可以使用idx_name

-- 创建反向索引（对左模糊的支持）
ALTER TABLE products ADD COLUMN name_reverse VARCHAR(200);
UPDATE products SET name_reverse = REVERSE(name);
CREATE INDEX idx_name_reverse ON products(name_reverse);

-- 查询左模糊
SELECT * FROM products WHERE name_reverse LIKE REVERSE('%MacBook');
```

### 解决方案4：组合索引优化

```sql
-- 如果模糊查询通常和其他条件一起使用
-- 例如：查询特定分类下名称包含关键词的商品

CREATE INDEX idx_category_status_name ON products(category, status, name(20));

-- 查询（只能使用前两个字段的索引，name的模糊查询无法使用索引）
SELECT * FROM products
WHERE category = '电子产品'
  AND status = 'ACTIVE'
  AND name LIKE '%MacBook%';

-- 优化：先使用索引过滤，再内存中筛选
SELECT * FROM products
WHERE category = '电子产品'
  AND status = 'ACTIVE'
  AND name LIKE '%MacBook%';  -- 使用索引过滤到几千条后，再LIKE筛选
```

**在本项目中：**

我们的 `idx_name_price` 组合索引：
```sql
-- 索引：(name, price)

-- 查询1（无法使用索引）
SELECT * FROM products WHERE name LIKE '%MacBook%';

-- 查询2（只能使用name部分索引）
SELECT * FROM products WHERE name LIKE 'MacBook%' ORDER BY price;

-- 查询3（无法使用索引）
SELECT * FROM products WHERE price BETWEEN 1000 AND 5000;
```

---

## 五、MySQL执行计划分析

### 使用EXPLAIN分析查询

```sql
-- 检查索引使用情况
EXPLAIN SELECT * FROM users WHERE username = 'john_doe';

-- 输出示例：
-- +----+-------------+-------+------------+-------+---------------+--------------+---------+-------+------+----------+-------+
-- | id | select_type | table | partitions | type  | possible_keys | key          | key_len | ref   | rows | filtered | Extra |
-- +----+-------------+-------+------------+-------+---------------+--------------+---------+-------+------+----------+-------+
-- | 1  | SIMPLE      | users | NULL       | const | uk_username   | uk_username  | 202     | const | 1    | 100.00   | NULL  |
-- +----+-------------+-------+------------+-------+---------------+--------------+---------+-------+------+----------+-------+

-- type = const: 使用了唯一索引，效率最高
-- key = uk_username: 实际使用的索引
-- rows = 1: 只扫描了1行
-- Extra = NULL: 没有额外操作（没有filesort等）
```

### type字段说明（从好到坏）

| type值 | 说明 | 性能 |
|--------|------|------|
| `system` | 系统表，只有一行 | ⭐⭐⭐⭐⭐ |
| `const` | 唯一索引等值查询 | ⭐⭐⭐⭐⭐ |
| `eq_ref` | 主键/唯一索引 JOIN | ⭐⭐⭐⭐ |
| `ref` | 普通索引等值查询 | ⭐⭐⭐ |
| `range` | 索引范围查询 | ⭐⭐⭐ |
| `index` | 全索引扫描 | ⭐⭐ |
| `ALL` | 全表扫描 | ⭐ |

### Extra字段说明

| Extra值 | 说明 | 优化建议 |
|---------|------|---------|
| `Using index` | 覆盖索引查询（不回表） | ✅ 良好 |
| `Using where` | WHERE条件过滤 | ✅ 正常 |
| `Using index condition` | 索引条件下推（ICP） | ✅ 良好 |
| `Using filesort` | 使用文件排序 | ❌ 需要优化，添加排序字段索引 |
| `Using temporary` | 使用临时表 | ❌ 需要优化，改进查询 |
| `Using join buffer` | 使用JOIN缓冲 | ⚠️ 注意，考虑优化JOIN |

---

## 六、项目中索引使用演示

### 1. 用户名精确查询（使用索引）

```sql
-- 索引：uk_username (username)
EXPLAIN SELECT * FROM users WHERE username = 'john_doe';

-- 预期：type = const, key = uk_username, rows = 1
```

### 2. 手机号精确查询（使用索引）

```sql
-- 索引：uk_phone (phone)
EXPLAIN SELECT * FROM users WHERE phone = '13800138000';

-- 预期：type = const, key = uk_phone, rows = 1
```

### 3. 按角色分页查询（使用组合索引）

```sql
-- 索引：idx_role_created_at (status, created_at)
EXPLAIN SELECT * FROM users
WHERE role = 'ADMIN'
ORDER BY created_at DESC
LIMIT 0, 20;

-- 预期：type = ref, key = idx_role_created_at
-- Extra: Using where; Using index（覆盖索引）
```

### 4. 价格范围查询（使用索引）

```sql
-- 索引：idx_price (price)
EXPLAIN SELECT * FROM products WHERE price BETWEEN 1000 AND 5000;

-- 预期：type = range, key = idx_price, rows = N
```

### 5. 查询可销售商品（使用组合索引）

```sql
-- 索引：idx_status_stock (status, stock)
EXPLAIN SELECT * FROM products
WHERE status = 'ACTIVE' AND stock > 0;

-- 预期：type = ref, key = idx_status_stock
```

### 6. 使用全文索引进行模糊查询

```sql
-- 全文索引：ft_name (name)
EXPLAIN SELECT * FROM products
WHERE MATCH(name) AGAINST('MacBook' IN BOOLEAN MODE);

-- 预期：type = fulltext, key = ft_name
```

---

## 七、索引优化检查清单

在创建索引前，请检查以下事项：

- [ ] 该查询的执行频率（低频查询可能不需要索引）
- [ ] 字段的区分度（区分度低的不适合单独索引）
- [ ] 查询条件是否使用函数（函数会导致索引失效）
- [ ] 组合索引的列顺序（最左前缀原则）
- [ ] 是否需要覆盖索引（避免回表）
- [ ] 索引的维护成本（写操作会变慢）
- [ ] 使用EXPLAIN分析查询计划
- [ ] 监控索引命中率（Handler_read_key）

---

## 八、MySQL性能监控

### 查看索引使用情况

```sql
-- 查看表的所有索引
SHOW INDEX FROM users;

-- 查看索引大小
SELECT table_name, index_name, ROUND(stat_value * @@innodb_page_size / 1024 / 1024, 2) AS size_mb
FROM mysql.innodb_index_stats
WHERE database_name = 'saas' AND table_name = 'users';
```

### 查看慢查询

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 记录超过1秒的查询

-- 查看慢查询
SELECT * FROM mysql.slow_log;

-- 使用mysqldumpslow分析
mysqldumpslow -s t /var/log/mysql/slow.log
```

### 查看索引命中率

```sql
SHOW GLOBAL STATUS LIKE 'Handler_read%';

-- Handler_read_key: 通过索引读取的次数
-- Handler_read_rnd_next: 全表扫描读取下一行的次数

-- 索引命中率 = Handler_read_key / (Handler_read_key + Handler_read_rnd_next)
```

---

## 总结

### 索引设计黄金法则

1. **只为频繁查询的字段创建索引**
2. **区分度低的字段单独建索引效果差**
3. **组合索引遵循最左前缀原则**
4. **范围查询的列放在组合索引最后**
5. **使用EXPLAIN分析所有慢查询**
6. **监控索引命中率，低于95%需要优化**
7. **索引不是越多越好，写操作会变慢**
8. **定期清理未使用的索引**

### 本项目索引统计

**User表：**
- 总索引数：10个
- 主键索引：1个
- 唯一索引：3个
- 单列索引：4个
- 组合索引：2个

**Product表：**
- 总索引数：9个
- 主键索引：1个
- 单列索引：5个
- 组合索引：3个
- 全文索引：1个

**索引覆盖场景：**
- ✅ 精确查询（username、phone、email）
- ✅ 范围查询（price、stock、created_at）
- ✅ 多条件查询（status + stock、status + price）
- ✅ 排序优化（status + created_at、status + price）
- ✅ 全文搜索（name FULLTEXT）

---

**文档最后更新：** 2026-03-18
**适用数据库：** MySQL 8.0+
**项目位置：** `/Users/mawenhui/Code/spring-demo/`
