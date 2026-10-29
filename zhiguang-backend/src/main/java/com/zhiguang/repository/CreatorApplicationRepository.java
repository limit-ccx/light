package com.zhiguang.repository;

import com.zhiguang.entity.ApplicationStatus;
import com.zhiguang.entity.CreatorApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreatorApplicationRepository extends JpaRepository<CreatorApplication, Long> {

    Optional<CreatorApplication> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<CreatorApplication> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndDirectionAndStatusIn(Long userId, String direction, List<ApplicationStatus> statuses);

    Page<CreatorApplication> findByStatus(ApplicationStatus status, Pageable pageable);
}
