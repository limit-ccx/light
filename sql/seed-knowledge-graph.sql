-- ================================================
-- 知识图谱 & 知识库 种子数据
-- ================================================

USE zhiguang;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ================================================
-- 1. 知识库分类
-- ================================================
INSERT INTO knowledge_categories (user_id, name, parent_id, icon, color, sort_order) VALUES
(1, '前端开发', NULL, 'code', '#42b883', 1),
(1, '后端开发', NULL, 'server', '#6db33f', 2),
(1, 'AI 与大模型', NULL, 'ai', '#9b59b6', 3),
(1, '架构设计', NULL, 'layers', '#f39c12', 4),
(1, '数据库', NULL, 'database', '#3498db', 5),
(2, 'React 生态', 1, 'react', '#61dafb', 1),
(2, 'Vue 生态', 1, 'vue', '#42b883', 2),
(2, 'Spring Boot', 2, 'spring', '#6db33f', 1),
(2, '微服务', 2, 'microservices', '#9b59b6', 2),
(2, 'LLM 基础', 3, 'brain', '#e74c3c', 1),
(2, 'RAG 应用', 3, 'books', '#9b59b6', 2);

-- ================================================
-- 2. 知识图谱节点
-- ================================================
INSERT INTO knowledge_nodes (name, type, properties) VALUES
('Vue 3', 'tech', '{"category":"frontend","version":"3.x"}'),
('React', 'tech', '{"category":"frontend","version":"18.x"}'),
('Spring Boot', 'tech', '{"category":"backend","version":"3.x"}'),
('TypeScript', 'tech', '{"category":"language"}'),
('大语言模型', 'concept', '{"category":"ai"}'),
('RAG', 'concept', '{"category":"ai","fullname":"Retrieval-Augmented Generation"}'),
('向量数据库', 'concept', '{"category":"ai"}'),
('MySQL', 'tech', '{"category":"database"}'),
('PostgreSQL', 'tech', '{"category":"database"}'),
('Redis', 'tech', '{"category":"database"}'),
('Docker', 'tech', '{"category":"devops"}'),
('Kubernetes', 'tech', '{"category":"devops"}'),
('微服务', 'concept', '{"category":"architecture"}'),
('单体架构', 'concept', '{"category":"architecture"}'),
('组合式 API', 'concept', '{"category":"frontend","framework":"Vue"}'),
('JWT', 'concept', '{"category":"security","fullname":"JSON Web Token"}'),
('BERT', 'tech', '{"category":"ai","type":"model"}'),
('LLaMA', 'tech', '{"category":"ai","type":"model"}'),
('Embedding', 'concept', '{"category":"ai"}'),
('Semantic Search', 'concept', '{"category":"ai"}'),
('函数式编程', 'concept', '{"category":"paradigm"}'),
('响应式编程', 'concept', '{"category":"paradigm"}'),
('Hibernate', 'tech', '{"category":"backend","framework":"Spring"}'),
('MyBatis', 'tech', '{"category":"backend"}'),
('MinIO', 'tech', '{"category":"storage","type":"object"}'),
('Elasticsearch', 'tech', '{"category":"search"}'),
('Prometheus', 'tech', '{"category":"monitoring"}'),
('Grafana', 'tech', '{"category":"monitoring"}'),
('SkyWalking', 'tech', '{"category":"monitoring","type":"APM"}'),
('RabbitMQ', 'tech', '{"category":"message-queue"}'),
('gRPC', 'tech', '{"category":"protocol"}'),
('GraphQL', 'tech', '{"category":"api"}'),
('RESTful', 'concept', '{"category":"api"}'),
('Vite', 'tech', '{"category":"build-tool"}'),
('Maven', 'tech', '{"category":"build-tool"}'),
('Node.js', 'tech', '{"category":"runtime"}'),
('Java', 'tech', '{"category":"language"}'),
('Python', 'tech', '{"category":"language"}');

-- ================================================
-- 3. 知识图谱边
-- ================================================
INSERT INTO knowledge_edges (source_id, target_id, relation, weight) VALUES
-- Vue 生态
(1, 15, 'uses', 1.0), (1, 4, 'uses', 0.9), (1, 34, 'built_with', 0.8),
-- React 生态
(2, 4, 'uses', 0.9), (2, 36, 'runs_on', 1.0),
-- Spring Boot 生态
(3, 37, 'uses', 1.0), (3, 23, 'integrates', 0.9), (3, 8, 'integrates', 0.8), (3, 10, 'integrates', 0.7), (3, 16, 'implements', 0.9),
-- AI 相关
(5, 17, 'includes', 0.9), (5, 18, 'includes', 0.9), (6, 5, 'based_on', 1.0), (6, 7, 'uses', 1.0), (6, 19, 'uses', 1.0),
(7, 9, 'stores', 0.9), (19, 20, 'enables', 0.8),
-- 架构
(13, 3, 'uses', 0.8), (13, 12, 'orchestrated_by', 0.9), (14, 13, 'opposite_of', 0.5),
-- 前端范式
(1, 22, 'implements', 0.9), (2, 22, 'implements', 0.9), (15, 22, 'enables', 1.0), (1, 21, 'supports', 0.7),
-- 存储与搜索
(8, 24, 'connected_to', 0.5), (9, 24, 'connected_to', 0.5), (26, 20, 'powers', 0.9), (9, 7, 'extension', 1.0),
-- DevOps & 监控
(11, 12, 'packages_for', 1.0), (27, 28, 'integrates', 1.0), (29, 13, 'monitors', 0.9),
-- 消息与协议
(30, 13, 'enables', 0.9), (31, 13, 'alternative_to', 0.6), (32, 33, 'alternative_to', 0.6),
-- 语言
(37, 3, 'runs', 1.0), (37, 23, 'runs', 1.0), (38, 36, 'runs', 0.8);

-- ================================================
-- 4. 知识库条目（从现有文章缓存）
-- ================================================
INSERT INTO knowledge_items (user_id, cached_title, cached_content, cached_summary, cached_tags, cached_author_name, category_id, notes, original_article_id, read_status)
SELECT
    a.user_id,
    a.title,
    a.content,
    a.summary,
    JSON_ARRAYAGG(t.tag),
    u.username,
    CASE
        WHEN a.title LIKE '%Vue%' OR a.title LIKE '%前端%' THEN 2
        WHEN a.title LIKE '%React%' THEN 6
        WHEN a.title LIKE '%Spring%' OR a.title LIKE '%Java%' THEN 8
        WHEN a.title LIKE '%Python%' THEN 5
        ELSE 1
    END,
    CONCAT('知识点: ', a.title),
    a.id,
    'UNREAD'
FROM articles a
JOIN users u ON u.id = a.user_id
LEFT JOIN article_tags t ON t.article_id = a.id
GROUP BY a.id
LIMIT 10;

SET FOREIGN_KEY_CHECKS = 1;
SELECT "知识图谱种子数据导入完成" AS result;
