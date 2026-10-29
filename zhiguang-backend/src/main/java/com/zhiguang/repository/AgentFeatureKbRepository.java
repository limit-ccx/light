package com.zhiguang.repository;

import com.zhiguang.entity.AgentFeatureKb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentFeatureKbRepository extends JpaRepository<AgentFeatureKb, Long> {

    List<AgentFeatureKb> findByEnabledTrueOrderBySortOrderAsc();
}
