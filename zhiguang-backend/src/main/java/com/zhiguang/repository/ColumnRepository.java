package com.zhiguang.repository;

import com.zhiguang.entity.ColumnEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColumnRepository extends JpaRepository<ColumnEntity, Long> {

    Page<ColumnEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<ColumnEntity> findByUserId(Long userId);
}
