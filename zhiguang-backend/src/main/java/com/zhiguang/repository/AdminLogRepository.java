package com.zhiguang.repository;

import com.zhiguang.entity.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    Page<AdminLog> findByAction(String action, Pageable pageable);

    Page<AdminLog> findByAdminId(Long adminId, Pageable pageable);

    @Query("SELECT l FROM AdminLog l WHERE " +
            "(:action IS NULL OR l.action = :action) " +
            "AND (:start IS NULL OR l.createdAt >= :start) " +
            "AND (:end IS NULL OR l.createdAt < :end)")
    Page<AdminLog> searchLogs(
            @Param("action") String action,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
