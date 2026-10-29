package com.zhiguang.repository;

import com.zhiguang.entity.KnowledgeEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface KnowledgeEdgeRepository extends JpaRepository<KnowledgeEdge, Long> {

    List<KnowledgeEdge> findBySourceId(Long sourceId);

    List<KnowledgeEdge> findByTargetId(Long targetId);

    List<KnowledgeEdge> findBySourceIdAndTargetId(Long sourceId, Long targetId);

    void deleteBySourceId(Long sourceId);

    void deleteByTargetId(Long targetId);

    /**
     * 批量删除两端节点相关的边
     */
    void deleteBySourceIdIn(Collection<Long> nodeIds);

    void deleteByTargetIdIn(Collection<Long> nodeIds);

    /**
     * 查找指定节点集合相关的所有边
     */
    List<KnowledgeEdge> findBySourceIdInOrTargetIdIn(Collection<Long> sourceIds, Collection<Long> targetIds);

    /**
     * 根据源节点和目标节点集合查找边
     */
    List<KnowledgeEdge> findBySourceIdInAndTargetIdIn(Collection<Long> sourceIds, Collection<Long> targetIds);
}
