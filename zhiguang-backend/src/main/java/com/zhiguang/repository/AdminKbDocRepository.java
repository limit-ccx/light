package com.zhiguang.repository;

import com.zhiguang.entity.AdminKbDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminKbDocRepository extends JpaRepository<AdminKbDoc, Long> {
}
