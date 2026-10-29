package com.zhiguang.repository;

import com.zhiguang.entity.ExperienceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceLogRepository extends JpaRepository<ExperienceLog, Long> {

    List<ExperienceLog> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    Page<ExperienceLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
