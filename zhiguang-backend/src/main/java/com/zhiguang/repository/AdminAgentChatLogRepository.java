package com.zhiguang.repository;

import com.zhiguang.entity.AdminAgentChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAgentChatLogRepository extends JpaRepository<AdminAgentChatLog, Long> {

    Page<AdminAgentChatLog> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);
}
