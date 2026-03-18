package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository接口
 * 继承JpaRepository，获得基本的CRUD功能
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户（精确匹配）
     * @param username 用户名
     * @return Optional包装的用户对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据用户名模糊查询（不区分大小写）
     * @param username 用户名关键词
     * @return 用户列表
     */
    List<User> findByUsernameContainingIgnoreCase(String username);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return Optional包装的用户对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户
     * @param phone 手机号
     * @return Optional包装的用户对象
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据角色查找用户
     * @param role 用户角色
     * @return 用户列表
     */
    List<User> findByRole(Role role);

    /**
     * 根据角色分页查询用户
     * @param role 用户角色
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * 根据用户名或邮箱或手机号查找用户
     * @param username 用户名
     * @param email 邮箱
     * @param phone 手机号
     * @return Optional包装的用户对象
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email OR u.phone = :phone")
    Optional<User> findByUsernameOrEmailOrPhone(@Param("username") String username,
                                                @Param("email") String email,
                                                @Param("phone") String phone);

    /**
     * 根据用户名模糊查询（分页）
     * @param username 用户名关键词
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     * @param phone 手机号
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByPhone(String phone);

    /**
     * 删除用户（批量）
     * @param ids 用户ID列表
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.id IN :ids")
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据创建时间范围查询用户
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 用户列表
     */
    List<User> findByCreatedAtBetween(java.time.LocalDateTime startDate,
                                      java.time.LocalDateTime endDate);

    /**
     * 根据创建时间范围分页查询用户
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    Page<User> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                      @Param("endDate") java.time.LocalDateTime endDate,
                                      Pageable pageable);
}
