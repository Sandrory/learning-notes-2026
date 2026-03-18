# SaaS基础系统分页查询API文档

## 📌 概述

为User和Product实体提供了完整的分页查询接口，采用Spring Data JPA Pageable，
结合@PreAuthorize实现细粒度的权限控制。

---

## 分页查询通用参数

所有分页查询接口都接受以下通用参数：

| 参数 | 类型 | 必需 | 默认值 | 描述 |
|------|------|------|--------|------|
| `page` | Integer | 否 | 0 | 页码（从0开始） |
| `size` | Integer | 否 | 10 | 每页大小（最多100） |
| `sortBy` | String | 否 | id/createdAt | 排序字段 |
| `sortDirection` | String | 否 | asc/desc | 排序方向 |

---

## 👤 用户管理API

### 基础信息
- **基础路径**: `/api/users`
- **权限要求**: 需要认证（不同接口有不同角色限制）

### 1. 分页查询用户

**接口**: `GET /api/users/page`

**权限**: `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")`
- **ADMIN**: 可以查询所有用户
- **USER**: 只能查询自己的信息

**请求参数**:

| 参数 | 类型 | 必需 | 描述 |
|------|------|------|------|
| `strategy` | String | 是 | 查询策略（见下方支持的策略） |
| `params` | String | 否 | 查询参数（根据strategy变化） |

**支持的查询策略**:

| 策略名称 | 描述 | 参数示例 |
|---------|------|---------|
| `userById` | 根据ID查询 | `params=1` |
| `userByUsername` | 根据用户名查询 | `params=john` |
| `userByEmail` | 根据邮箱查询 | `params=john@example.com` |
| `userByPhone` | 根据手机号查询 | `params=13800138000` |
| `usersByRole` | 根据角色查询 | `params=ADMIN` |
| `usersByUsernameLike` | 用户名模糊查询 | `params=john` |
| `usersByUsernameOrEmailOrPhone` | 多条件查询 | `params=john` |
| `allUsers` | 查询所有用户 | 无需params |

**请求示例**:
```bash
# ADMIN查询所有用户（第0页，每页10条）
GET /api/users/page?strategy=allUsers&page=0&size=10

# 根据角色查询（需要ADMIN权限）
GET /api/users/page?strategy=usersByRole&params=ADMIN&page=0&size=10

# 根据ID查询（USER只能查到自己）
GET /api/users/page?strategy=userById&params=1&page=0&size=10
```

**响应格式**:
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
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

### 2. 根据ID查询用户

**接口**: `GET /api/users/id?id={id}`

**权限**: `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")`

**权限控制**: USER只能查询自己的信息，ADMIN可以查询所有用户

**响应格式**: 单个User对象

### 3. 根据角色查询用户

**接口**: `GET /api/users/by-role?role={role}`

**权限**: `@PreAuthorize("hasRole('ADMIN')")` ⭐ 仅限ADMIN

**参数**: `role` - 角色名称（USER/ADMIN/SUPER_ADMIN）

### 4. 查询当前登录用户

**接口**: `GET /api/users/current`

**权限**: `@PreAuthorize("isAuthenticated()")`

**描述**: 获取当前认证用户的信息

---

## 📦 商品管理API

### 基础信息
- **基础路径**: `/api/products`
- **权限要求**: 需要认证（isAuthenticated）

### 1. 分页查询商品

**接口**: `GET /api/products/page`

**权限**: `@PreAuthorize("isAuthenticated()")`

**请求参数**:

| 参数 | 类型 | 必需 | 描述 |
|------|------|------|------|
| `strategy` | String | 是 | 查询策略（见下方） |
| `params` | String | 否 | 查询参数（根据strategy变化） |

**支持的查询策略**:

| 策略名称 | 描述 | 参数示例 |
|---------|------|---------|
| `productById` | 根据ID查询 | `params=1` |
| `productByName` | 根据名称查询 | `params=商品名` |
| `productsByStatus` | 根据状态查询 | `params=ACTIVE` |
| `productsByNameLike` | 名称模糊查询 | `params=电脑` |
| `productsByPriceRange` | 价格范围查询 | `params=100-500` |
| `productsByStock` | 库存查询 | `params=50` |
| `productsActiveForSale` | 可销售商品查询 | 无需params |
| `productsNeedReorder` | 库存预警查询 | `params=10` |
| `allProducts` | 查询所有商品 | 无需params |

**请求示例**:
```bash
# 查询所有商品（第0页，每页20条，按价格降序）
GET /api/products/page?strategy=allProducts&page=0&size=20&sortBy=price&sortDirection=desc

# 根据状态查询（上架商品）
GET /api/products/page?strategy=productsByStatus&params=ACTIVE&page=0&size=10

# 价格范围查询（100-500元）
GET /api/products/page?strategy=productsByPriceRange&params=100-500&page=0&size=10

# 查询可销售的商品（有库存且上架）
GET /api/products/page?strategy=productsActiveForSale&page=0&size=10

# 库存预警查询（库存低于10个）
GET /api/products/page?strategy=productsNeedReorder&params=10&page=0&size=10
```

**响应格式**:
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
  "number": 0,
  "size": 10
  // ... 其他分页字段
}
```

### 2. 根据ID查询商品

**接口**: `GET /api/products/id?id={id}`

**权限**: `@PreAuthorize("isAuthenticated()")`

**描述**: 根据商品ID查询详细信息

### 3. 根据状态查询商品

**接口**: `GET /api/products/by-status?status={status}`

**权限**: `@PreAuthorize("isAuthenticated()")`

**参数**: `status` - 商品状态（ACTIVE/INACTIVE/OUT_OF_STOCK/DELETED）

### 4. 查询可销售的商品

**接口**: `GET /api/products/active-for-sale`

**权限**: `@PreAuthorize("isAuthenticated()")`

**描述**: 查询所有可销售的商品（状态为上架且有库存）

### 5. 库存预警查询

**接口**: `GET /api/products/need-reorder?threshold={threshold}`

**权限**: `@PreAuthorize("isAuthenticated()")`

**参数**: `threshold` - 库存阈值（默认10）

**描述**: 查询库存低于阈值的商品

---

## 🔐 权限控制详解

### @PreAuthorize注解使用

#### 1. 用户查询权限
```java
// 允许ADMIN和USER查询
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public Page<UserResponse> findUsers(...) {
    // 方法内部实现权限过滤
}
```

**权限控制逻辑**:
- **ADMIN角色**: 可以查询所有用户，不受限制
- **USER角色**: 只能查询自己的信息，系统会自动过滤

#### 2. 商品查询权限
```java
// 任何认证用户都可以查询商品
@PreAuthorize("isAuthenticated()")
public Page<ProductResponse> findProducts(...) {
    // 无额外限制
}
```

#### 3. 特殊权限
```java
// 仅限ADMIN
@PreAuthorize("hasRole('ADMIN')")
public Page<UserResponse> findUsersByRole(...) {
    // ...
}
```

### 权限过滤实现（Service层）

```java
@Override
public Page<User> findUsers(Pageable pageable, String strategyName, Object... params) {
    // 获取当前登录用户
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User currentUser = getCurrentUserFromAuthentication(authentication);

    // 查询数据
    QueryStrategy<User> strategy = userQueryFactory.createStrategy(strategyName);
    Page<User> result = strategy.executeQuery(pageable, params);

    // 权限过滤：USER只能查自己
    if (currentUser != null && currentUser.getRole() == Role.USER) {
        return filterUsersByPermission(result, currentUser);
    }

    // ADMIN返回所有数据
    return result;
}
```

---

## 🧪 测试示例

### 使用curl进行测试

```bash
# 1. 查询所有用户（需要ADMIN权限）
curl -u admin:password http://localhost:8080/api/users/page?strategy=allUsers

# 2. 根据ID查询用户（USER只能查自己）
curl -u user1:password http://localhost:8080/api/users/id?id=2

# 3. 查询所有商品
curl -u user1:password http://localhost:8080/api/products/page?strategy=allProducts&page=0&size=10

# 4. 按状态查询商品
curl -u user1:password http://localhost:8080/api/products/by-status?status=ACTIVE

# 5. 查询可销售的商品
curl -u user1:password http://localhost:8080/api/products/active-for-sale
```

---

## 📝 总结

### 已实现的功能

✅ **分页查询**: 使用Spring Data JPA Pageable实现
✅ **多种查询策略**: 16种查询策略（8用户 + 8商品）
✅ **权限控制**: @PreAuthorize + Service层过滤
✅ **开闭原则**: 新增查询策略无需修改Service代码
✅ **统一响应**: Page<DTO>格式

### 权限矩阵

| 接口 | USER | ADMIN | 说明 |
|------|------|-------|------|
| 查询所有用户 | ❌ 只能查自己 | ✅ | USER被限制 |
| 查询所有商品 | ✅ | ✅ | 无限制 |
| 按角色查询用户 | ❌ | ✅ | ADMIN专属 |
| 查询当前用户 | ✅ | ✅ | 都可以 |
| 查询可销售的商品 | ✅ | ✅ | 无限制 |

---

**文档位置**: `/Users/mawenhui/Code/spring-demo/API_ENDPOINTS.md`
**最后更新**: 2026-03-18
