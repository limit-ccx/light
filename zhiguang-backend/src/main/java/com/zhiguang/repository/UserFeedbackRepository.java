package com.zhiguang.repository;

import com.zhiguang.entity.UserFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    Page<UserFeedback> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<UserFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);
}
