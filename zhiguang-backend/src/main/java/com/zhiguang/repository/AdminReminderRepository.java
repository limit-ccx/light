package com.zhiguang.repository;

import com.zhiguang.entity.AdminReminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminReminderRepository extends JpaRepository<AdminReminder, Long> {

    Page<AdminReminder> findByToUserIdOrderByCreatedAtDesc(Long toUserId, Pageable pageable);

    Page<AdminReminder> findByToUserIdAndStatusOrderByCreatedAtDesc(Long toUserId, String status, Pageable pageable);

    Page<AdminReminder> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId, Pageable pageable);

    Page<AdminReminder> findByFromUserIdAndStatusOrderByCreatedAtDesc(Long fromUserId, String status, Pageable pageable);

    long countByToUserIdAndStatus(Long toUserId, String status);

    long countByFromUserIdAndStatusAndAcknowledgedAtIsNull(Long fromUserId, String status);

    long countByToUserIdAndReadAtIsNull(Long toUserId);

    List<AdminReminder> findByToUserIdAndReadAtIsNull(Long toUserId);
}
