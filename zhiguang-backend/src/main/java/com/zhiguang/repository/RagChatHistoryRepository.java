package com.zhiguang.repository;

import com.zhiguang.entity.RagChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagChatHistoryRepository extends JpaRepository<RagChatHistory, Long> {

    List<RagChatHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
