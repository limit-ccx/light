package com.zhiguang.repository;

import com.zhiguang.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** 按日统计新增用户（PERF-P2-2：替代"逐日循环 COUNT"的 N 次查询） */
    @Query(value = "SELECT DATE(created_at) AS d, COUNT(*) AS c FROM users " +
            "WHERE created_at >= :since GROUP BY DATE(created_at)", nativeQuery = true)
    List<Object[]> countDailyNewUsers(@Param("since") java.time.LocalDateTime since);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) AND u.id != :excludeUserId ORDER BY u.username ASC")
    org.springframework.data.domain.Page<User> findByUsernameContainingIgnoreCase(
            @Param("keyword") String keyword,
            @Param("excludeUserId") Long excludeUserId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
            "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
            "AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:status IS NULL OR u.status = :status)")
    org.springframework.data.domain.Page<User> searchForAdmin(
            @Param("username") String username,
            @Param("email") String email,
            @Param("role") String role,
            @Param("status") String status,
            org.springframework.data.domain.Pageable pageable);

    /** 运营管理模块：只查管理员账号，支持关键词与职位筛选 */
    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN' " +
            "AND (:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:position IS NULL OR u.adminPosition = :position)")
    org.springframework.data.domain.Page<User> searchAdmins(
            @Param("keyword") String keyword,
            @Param("position") String position,
            org.springframework.data.domain.Pageable pageable);

    long countByStatus(String status);

    long countByRole(String role);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.securityVersion = COALESCE(u.securityVersion, 0) + 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int incrementSecurityVersion(@Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = CURRENT_DATE")
    long countByCreatedToday();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :start AND u.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}