package com.zhiguang.repository;

import com.zhiguang.entity.TreeHoleReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TreeHoleReportRepository extends JpaRepository<TreeHoleReport, Long> {

    Optional<TreeHoleReport> findByAnonymousHashAndTargetTypeAndTargetId(String anonymousHash, String targetType, Long targetId);

    Page<TreeHoleReport> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    long countByAnonymousHashAndCreatedAtAfter(String anonymousHash, LocalDateTime after);
}
