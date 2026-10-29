package com.zhiguang.repository;

import com.zhiguang.entity.KnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, Long> {

    List<KnowledgeNode> findByNameContaining(String keyword);

    List<KnowledgeNode> findByType(String type);

    /**
     * 精确匹配节点名称
     */
    List<KnowledgeNode> findByName(String name);

    /**
     * 精确匹配节点名称和类型
     */
    List<KnowledgeNode> findByNameAndType(String name, String type);
}
