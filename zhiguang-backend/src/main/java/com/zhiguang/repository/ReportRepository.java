package com.zhiguang.repository;

import com.zhiguang.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByStatus(String status);

    Page<Report> findByStatus(String status, Pageable pageable);
}
