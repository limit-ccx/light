package com.zhiguang.service;

import com.zhiguang.entity.KnowledgeEdge;
import com.zhiguang.entity.KnowledgeNode;
import com.zhiguang.repository.KnowledgeEdgeRepository;
import com.zhiguang.repository.KnowledgeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final AiService aiService;

    private static final int MAX_GRAPH_NODES = 200;

    /** 从文章提取并保存知识实体 */
    @Transactional
    public Map<String, Object> extractAndSave(Long articleId, String content) {
        log.info("开始从文章 {} 提取知识实体", articleId);
        Map<String, Object> extracted = aiService.extractKnowledge(content);

        @SuppressWarnings("unchecked")
        List<String> concepts = (List<String>) extracted.getOrDefault("concepts", List.of());
        @SuppressWarnings("unchecked")
        List<String> persons = (List<String>) extracted.getOrDefault("persons", List.of());
        @SuppressWarnings("unchecked")
        List<String> techStack = (List<String>) extracted.getOrDefault("techStack", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> relations = (List<Map<String, String>>) extracted.getOrDefault("relations", List.of());

        // 保存节点
        Map<String, Long> savedNodeIds = new HashMap<>();
        for (String concept : concepts) {
            savedNodeIds.put(concept, saveOrGetNode(concept, "concept").getId());
        }
        for (String person : persons) {
            savedNodeIds.put(person, saveOrGetNode(person, "person").getId());
        }
        for (String tech : techStack) {
            savedNodeIds.put(tech, saveOrGetNode(tech, "tech").getId());
        }

        // 去重并保存边
        Set<String> processedPairs = new HashSet<>();
        List<KnowledgeEdge> edgesToSave = new ArrayList<>();
        for (Map<String, String> rel : relations) {
            String source = rel.get("source");
            String target = rel.get("target");
            String relation = rel.get("relation");
            if (source == null || target == null
                    || !savedNodeIds.containsKey(source) || !savedNodeIds.containsKey(target)) {
                continue;
            }
            Long sourceId = savedNodeIds.get(source);
            Long targetId = savedNodeIds.get(target);
            String pairKey = sourceId + "->" + targetId;
            if (processedPairs.contains(pairKey)) {
                continue;
            }
            processedPairs.add(pairKey);

            KnowledgeEdge edge = new KnowledgeEdge();
            edge.setSourceId(sourceId);
            edge.setTargetId(targetId);
            edge.setRelation(relation != null && !relation.isBlank() ? relation : "关联");
            edgesToSave.add(edge);
        }
        if (!edgesToSave.isEmpty()) {
            edgeRepository.saveAll(edgesToSave);
        }

        log.info("文章 {} 知识提取完成，新增/复用节点 {} 个，关系 {} 条",
                articleId, savedNodeIds.size(), edgesToSave.size());

        return Map.of(
                "nodes", Map.of("concepts", concepts, "persons", persons, "techStack", techStack),
                "edges", edgesToSave.stream()
                        .map(e -> Map.of(
                                "source", e.getSourceId(),
                                "target", e.getTargetId(),
                                "relation", e.getRelation()))
                        .toList(),
                "nodeCount", savedNodeIds.size(),
                "edgeCount", edgesToSave.size()
        );
    }

    /** 获取知识图谱数据，支持限制节点数量避免前端卡顿 */
    public Map<String, Object> getGraphData(Integer limit) {
        int nodeLimit = (limit == null || limit <= 0) ? MAX_GRAPH_NODES : Math.min(limit, 500);

        // 优先返回度数高的节点，避免数据量过大
        List<KnowledgeNode> allNodes = nodeRepository.findAll();
        Map<Long, Integer> degreeMap = computeDegreeMap(allNodes.stream().map(KnowledgeNode::getId).collect(Collectors.toSet()));
        List<KnowledgeNode> nodes = allNodes.stream()
                .sorted((a, b) -> degreeMap.getOrDefault(b.getId(), 0) - degreeMap.getOrDefault(a.getId(), 0))
                .limit(nodeLimit)
                .toList();

        Set<Long> nodeIds = nodes.stream().map(KnowledgeNode::getId).collect(Collectors.toSet());
        List<KnowledgeEdge> edges = edgeRepository.findBySourceIdInAndTargetIdIn(nodeIds, nodeIds);

        return Map.of(
                "nodes", nodes,
                "edges", edges,
                "totalNodes", allNodes.size(),
                "totalEdges", edgeRepository.count()
        );
    }

    /** 搜索节点 */
    public List<KnowledgeNode> searchNodes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return nodeRepository.findAll();
        }
        return nodeRepository.findByNameContaining(keyword.trim());
    }

    /** 获取节点详情（包含关联节点和边） */
    public Map<String, Object> getNodeDetail(Long id) {
        KnowledgeNode node = nodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));

        List<KnowledgeEdge> outgoingEdges = edgeRepository.findBySourceId(id);
        List<KnowledgeEdge> incomingEdges = edgeRepository.findByTargetId(id);

        Set<Long> relatedNodeIds = new HashSet<>();
        outgoingEdges.forEach(e -> relatedNodeIds.add(e.getTargetId()));
        incomingEdges.forEach(e -> relatedNodeIds.add(e.getSourceId()));

        Map<Long, KnowledgeNode> relatedNodeMap = nodeRepository.findAllById(relatedNodeIds)
                .stream().collect(Collectors.toMap(KnowledgeNode::getId, n -> n));

        // 直接序列化实体可能触发 Hibernate 代理异常，这里统一转成普通 Map
        Map<Long, KnowledgeNode> edgeNodeMap = new HashMap<>(relatedNodeMap);
        edgeNodeMap.put(node.getId(), node);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("node", toNodeMap(node));
        detail.put("outgoingEdges", enrichEdges(outgoingEdges, edgeNodeMap));
        detail.put("incomingEdges", enrichEdges(incomingEdges, edgeNodeMap));
        detail.put("relatedNodes", relatedNodeMap.values().stream().map(this::toNodeMap).toList());
        return detail;
    }

    private Map<String, Object> toNodeMap(KnowledgeNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.getId());
        map.put("name", node.getName());
        map.put("type", node.getType());
        map.put("properties", node.getProperties());
        map.put("createdAt", node.getCreatedAt());
        return map;
    }

    /** 获取按类型分组的节点 */
    public Map<String, List<KnowledgeNode>> getNodesByType() {
        List<KnowledgeNode> allNodes = nodeRepository.findAll();
        return allNodes.stream()
                .collect(Collectors.groupingBy(
                        n -> n.getType() != null ? n.getType() : "unknown"
                ));
    }

    /** 删除节点（同时删除相关边） */
    @Transactional
    public void deleteNode(Long id) {
        if (!nodeRepository.existsById(id)) {
            throw new IllegalArgumentException("节点不存在");
        }
        edgeRepository.deleteBySourceId(id);
        edgeRepository.deleteByTargetId(id);
        nodeRepository.deleteById(id);
        log.info("删除节点 {} 及其关联边", id);
    }

    /** 删除边 */
    public void deleteEdge(Long id) {
        if (!edgeRepository.existsById(id)) {
            throw new IllegalArgumentException("边不存在");
        }
        edgeRepository.deleteById(id);
    }

    /** 手动添加节点 */
    @Transactional
    public KnowledgeNode addNode(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("节点名称不能为空");
        }
        String cleanName = name.trim();
        List<KnowledgeNode> existing = nodeRepository.findByNameAndType(cleanName, type);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        KnowledgeNode node = new KnowledgeNode();
        node.setName(cleanName);
        node.setType(type);
        return nodeRepository.save(node);
    }

    /** 手动添加边 */
    @Transactional
    public Map<String, Object> addEdge(Long sourceId, Long targetId, String relation) {
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("源节点和目标节点不能相同");
        }
        KnowledgeNode source = nodeRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("源节点不存在"));
        KnowledgeNode target = nodeRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("目标节点不存在"));

        List<KnowledgeEdge> existing = edgeRepository.findBySourceIdAndTargetId(sourceId, targetId);
        if (!existing.isEmpty()) {
            return Map.of("edge", existing.get(0), "message", "边已存在");
        }

        KnowledgeEdge edge = new KnowledgeEdge();
        edge.setSourceId(sourceId);
        edge.setTargetId(targetId);
        edge.setRelation(relation != null && !relation.isBlank() ? relation : "关联");
        edge = edgeRepository.save(edge);

        return Map.of(
                "edge", edge,
                "sourceName", source.getName(),
                "targetName", target.getName()
        );
    }

    /** 获取统计信息 */
    public Map<String, Object> getStats() {
        long totalNodes = nodeRepository.count();
        long totalEdges = edgeRepository.count();

        Map<String, List<KnowledgeNode>> byType = getNodesByType();
        Map<String, Long> typeCounts = new HashMap<>();
        byType.forEach((type, nodes) -> typeCounts.put(type, (long) nodes.size()));

        return Map.of(
                "totalNodes", totalNodes,
                "totalEdges", totalEdges,
                "nodesByType", typeCounts
        );
    }

    private KnowledgeNode saveOrGetNode(String name, String type) {
        String cleanName = name.trim();
        List<KnowledgeNode> existing = nodeRepository.findByNameAndType(cleanName, type);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        KnowledgeNode node = new KnowledgeNode();
        node.setName(cleanName);
        node.setType(type);
        return nodeRepository.save(node);
    }

    private Map<Long, Integer> computeDegreeMap(Set<Long> nodeIds) {
        List<KnowledgeEdge> allEdges = edgeRepository.findBySourceIdInOrTargetIdIn(nodeIds, nodeIds);
        Map<Long, Integer> degreeMap = new HashMap<>();
        for (KnowledgeEdge edge : allEdges) {
            degreeMap.merge(edge.getSourceId(), 1, Integer::sum);
            degreeMap.merge(edge.getTargetId(), 1, Integer::sum);
        }
        return degreeMap;
    }

    private List<Map<String, Object>> enrichEdges(List<KnowledgeEdge> edges, Map<Long, KnowledgeNode> nodeMap) {
        return edges.stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", e.getId());
            map.put("sourceId", e.getSourceId());
            map.put("targetId", e.getTargetId());
            map.put("relation", e.getRelation());
            map.put("sourceName", nodeMap.get(e.getSourceId()) != null ? nodeMap.get(e.getSourceId()).getName() : "");
            map.put("targetName", nodeMap.get(e.getTargetId()) != null ? nodeMap.get(e.getTargetId()).getName() : "");
            return map;
        }).toList();
    }
}