package com.zhiguang.repository;

import com.zhiguang.entity.AgentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentFeedbackRepository extends JpaRepository<AgentFeedback, Long> {
}
