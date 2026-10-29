package com.zhiguang.repository;

import com.zhiguang.entity.ColumnSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColumnSubscriptionRepository extends JpaRepository<ColumnSubscription, Long> {

    boolean existsByUserIdAndColumnId(Long userId, Long columnId);

    void deleteByUserIdAndColumnId(Long userId, Long columnId);

    long countByColumnId(Long columnId);

    Page<ColumnSubscription> findByColumnId(Long columnId, Pageable pageable);

    List<ColumnSubscription> findByUserId(Long userId);

    Page<ColumnSubscription> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
