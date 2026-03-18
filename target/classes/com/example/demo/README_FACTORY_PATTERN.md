# 工厂模式设计说明

本设计采用**策略模式 + 工厂模式**的组合，实现符合**开闭原则**的服务层查询功能。

## 一、目录结构

```
service/
├── UserService.java                              # 服务接口
├── ProductService.java                           # 服务接口
├── impl/                                         # 服务实现
│   ├── UserServiceImpl.java
│   └── ProductServiceImpl.java
├── strategy/                                     # 查询策略
│   ├── QueryStrategy.java                       # 策略接口（通用）
│   ├── UserConstants.java                       # 用户查询策略常量
│   ├── ProductConstants.java                    # 商品查询策略常量
│   ├── factory/
│   │   ├── QueryStrategyFactory.java            # 工厂接口
│   │   └── DefaultQueryStrategyFactory.java     # 工厂实现
│   └── impl/
│       ├── UserByIdStrategy.java               # ID查询策略
│       ├── UserByUsernameStrategy.java         # 用户名查询策略
│       ├── UserByRoleStrategy.java            # 角色查询策略
│       ├── ProductByIdStrategy.java           # 商品ID查询策略
│       └── ProductsByStatusStrategy.java      # 商品状态查询策略
```

## 二、核心设计

### 1. QueryStrategy<T> - 策略接口
```java
public interface QueryStrategy<T> {
    List<T> executeQuery(Object... params);          // 不分页查询
    Page<T> executeQuery(Pageable pageable, Object... params);  // 分页查询
    String getStrategyName();                        // 策略名称
}
```

### 2. QueryStrategyFactory<T> - 工厂接口
```java
public interface QueryStrategyFactory<T> {
    QueryStrategy<T> createStrategy(String strategyName);  // 创建策略
    boolean containsStrategy(String strategyName);          // 检查策略
}
```

### 3. 使用常量管理策略名称
- **UserConstants**: 用户查询策略常量（8个）
- **ProductConstants**: 商品查询策略常量（8个）

## 三、开闭原则实现

### ✅ 符合开闭原则
- **对扩展开放**：新增查询类型只需新增实现类
- **对修改关闭**：无需修改Service层代码

### 💡 示例：新增查询类型
假设需要添加"根据创建时间查询用户"功能：

**步骤1：新增策略实现类**
```java
@Component
public class UsersByCreatedAtStrategy implements QueryStrategy<User> {
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<User> executeQuery(Object... params) {
        // 实现查询逻辑
        LocalDateTime start = (LocalDateTime) params[0];
        LocalDateTime end = (LocalDateTime) params[1];
        return userRepository.findByCreatedAtBetween(start, end);
    }

    @Override
    public String getStrategyName() {
        return "usersByCreatedAt";
    }
}
```

**步骤2：在UserConstants中添加常量**
```java
public static final String USER_QUERY_BY_CREATED_AT = "usersByCreatedAt";
```

**步骤3：Spring容器自动注册**
无需修改ServiceImpl，Spring会自动将新策略注入到工厂中！

## 四、Service层使用示例

```java
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private QueryStrategyFactory<User> userQueryFactory;

    public User findUserById(Long id) {
        // 使用工厂创建策略
        QueryStrategy<User> strategy = userQueryFactory
            .createStrategy(UserConstants.USER_QUERY_BY_ID);

        // 执行查询
        List<User> users = strategy.executeQuery(id);
        return users.isEmpty() ? null : users.get(0);
    }

    public Page<User> findUsersByRole(Role role, Pageable pageable) {
        // 工厂模式 + 策略模式
        QueryStrategy<User> strategy = userQueryFactory
            .createStrategy(UserConstants.USER_QUERY_BY_ROLE);

        return strategy.executeQuery(pageable, role);
    }
}
```

## 五、已实现的策略

### User查询策略（8个）
| 策略名称 | 策略类 | 描述 |
|---------|--------|------|
| `userById` | UserByIdStrategy | 根据ID查询 |
| `userByUsername` | UserByUsernameStrategy | 根据用户名查询 |
| `userByEmail` | UserByEmailStrategy | 根据邮箱查询 |
| `userByPhone` | UserByPhoneStrategy | 根据手机号查询 |
| `usersByRole` | UsersByRoleStrategy | 根据角色查询 |
| `usersByUsernameLike` | UsersByUsernameLikeStrategy | 用户名模糊查询 |
| `usersByUsernameOrEmailOrPhone` | UsersByUsernameOrEmailOrPhoneStrategy | 多条件查询 |
| `allUsers` | AllUsersStrategy | 查询所有用户 |

### Product查询策略（8个）
| 策略名称 | 策略类 | 描述 |
|---------|--------|------|
| `productById` | ProductByIdStrategy | 根据ID查询 |
| `productByName` | ProductByNameStrategy | 根据名称查询 |
| `productsByStatus` | ProductsByStatusStrategy | 根据状态查询 |
| `productsByNameLike` | ProductsByNameLikeStrategy | 名称模糊查询 |
| `productsByPriceRange` | ProductsByPriceRangeStrategy | 价格范围查询 |
| `productsByStock` | ProductsByStockStrategy | 库存查询 |
| `productsActiveForSale` | ProductsActiveForSaleStrategy | 可销售商品查询 |
| `productsNeedReorder` | ProductsNeedReorderStrategy | 库存预警查询 |
| `allProducts` | AllProductsStrategy | 查询所有商品 |

## 六、核心优势

### 1. 高扩展性
- 新增查询无需修改Service代码
- Spring自动注册新策略到工厂
- 配置化管理查询类型

### 2. 低耦合度
- Service只依赖策略接口
- 依赖注入解耦具体实现
- 易于单元测试（可Mock策略）

### 3. 类型安全
- 使用泛型`QueryStrategy<T>`保证类型安全
- 编译期检查，减少运行时错误

### 4. 可维护性
- 每个策略独立实现，职责单一
- 易于理解和维护
- 符合单一职责原则

## 七、使用步骤示例

### 1. 查询单个用户
```java
QueryStrategy<User> strategy = userQueryFactory
    .createStrategy(UserConstants.USER_QUERY_BY_ID);
User user = strategy.executeQuery(1L).get(0);
```

### 2. 查询所有用户（分页）
```java
QueryStrategy<User> strategy = userQueryFactory
    .createStrategy(UserConstants.USER_QUERY_BY_ROLE);
Page<User> users = strategy.executeQuery(pageable, Role.ADMIN);
```

### 3. 新增查询类型
```java
String oldWay = "需要修改ServiceImpl代码"; ❌

String newWay = "只需新增策略实现"; ✅
```

## 八、设计模式总结

| 设计模式 | 应用位置 | 作用 |
|---------|---------|------|
| **策略模式** | QueryStrategy接口及实现类 | 封装算法，可互换 |
| **工厂模式** | QueryStrategyFactory接口及实现 | 创建对象，解耦创建和使用 |
| **依赖注入** | Spring容器管理Bean | 依赖抽象而非具体实现 |
| **开闭原则** | 整体架构设计 | 对扩展开放，对修改关闭 |

---

**结论**：通过策略+工厂的组合模式，实现了高度可扩展、低耦合的服务层架构，完全符合开闭原则！
