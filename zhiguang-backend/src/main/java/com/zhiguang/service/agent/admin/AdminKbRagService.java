package com.zhiguang.service.agent.admin;

import com.zhiguang.config.AdminAgentConfig;
import com.zhiguang.entity.AdminKbDoc;
import com.zhiguang.repository.AdminKbDocRepository;
import com.zhiguang.service.AiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 RAG：文档分块 + 真实 embedding + pgvector 检索（设计方案 §3）。
 * <p>
 * 原文存 MySQL（admin_kb_docs），分块向量存 pgvector（admin_kb_chunks）。
 * 分块策略：按 Markdown 标题边界优先，超长段按 chunk-size 滑窗，相邻块重叠 chunk-overlap。
 * 首次启动播种内置运营知识（审核政策要点/数据口径/操作口径），后续由管理端 CRUD 维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKbRagService {

    private final JdbcTemplate vectorJdbcTemplate;
    private final AdminKbDocRepository docRepository;
    private final AiService aiService;
    private final com.zhiguang.config.AiConfig aiConfig;
    private final AdminAgentConfig config;

    @PostConstruct
    public void initTable() {
        try {
            // 自保护：本 Bean 初始化可能早于 VectorSearchService 的建扩展逻辑，
            // 先确保 pgvector 扩展存在（幂等），否则 vector 类型未知导致建表失败
            vectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            vectorJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS admin_kb_chunks (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "doc_id BIGINT NOT NULL, " +
                    "chunk_index INT NOT NULL, " +
                    "title TEXT, " +
                    "content TEXT, " +
                    "embedding vector(" + aiConfig.getEmbedding().getDimension() + "), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            vectorJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS admin_kb_chunk_embedding_idx " +
                    "ON admin_kb_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)");
            vectorJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS admin_kb_chunk_doc_idx " +
                    "ON admin_kb_chunks (doc_id)");
            seedIfEmpty();
        } catch (Exception e) {
            log.warn("Admin KB 向量表初始化失败: {}", e.getMessage());
        }
    }

    /** 文档重新索引：删旧块 → 分块 → embedding → 入 pgvector（异步） */
    @Async
    public void reindexAsync(Long docId) {
        try {
            reindex(docId);
        } catch (Exception e) {
            log.error("Admin KB 文档索引失败: docId={}", docId, e);
            docRepository.findById(docId).ifPresent(d -> {
                d.setStatus("FAILED");
                docRepository.save(d);
            });
        }
    }

    public int reindex(Long docId) {
        AdminKbDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + docId));
        vectorJdbcTemplate.update("DELETE FROM admin_kb_chunks WHERE doc_id = ?", docId);

        List<String> chunks = splitIntoChunks(doc.getContent(), doc.getTitle());
        int done = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            List<Double> vector = aiService.generateEmbedding(chunk);
            if (vector == null || vector.isEmpty()) {
                continue;
            }
            vectorJdbcTemplate.update(
                    "INSERT INTO admin_kb_chunks (doc_id, chunk_index, title, content, embedding) " +
                            "VALUES (?, ?, ?, ?, ?::vector)",
                    docId, i, doc.getTitle(), chunk, toVectorString(vector));
            done++;
        }
        doc.setStatus(done > 0 ? "INDEXED" : "FAILED");
        doc.setChunkCount(done);
        docRepository.save(doc);
        log.info("Admin KB 文档索引完成: docId={}, chunks={}/{}", docId, done, chunks.size());
        return done;
    }

    /**
     * 向量检索：top-k 片段（带文档标题），低于 minScore 丢弃。
     * 返回 [{"title","content","score"}]；无命中返回空列表（明确的不命中信号，§13.2）。
     */
    public List<Map<String, Object>> search(String query) {
        // 无真实 embedding 服务时（开发兜底为哈希向量，短查询 vs 长文档相似度不可用），
        // 降级为关键词检索：诚实可用；配置 AI_EMBEDDING_API_KEY 后自动走向量检索
        if (aiConfig.getEmbedding().getApiKey() == null || aiConfig.getEmbedding().getApiKey().isBlank()) {
            return keywordSearch(query);
        }
        List<Double> vector = aiService.generateEmbedding(query);
        if (vector == null || vector.isEmpty()) {
            return List.of();
        }
        var rag = config.getRag();
        try {
            String vectorStr = toVectorString(vector);
            return vectorJdbcTemplate.queryForList(
                    "SELECT title, content, 1 - (embedding <=> ?::vector) AS score " +
                            "FROM admin_kb_chunks " +
                            "WHERE 1 - (embedding <=> ?::vector) > ? " +
                            "ORDER BY embedding <=> ?::vector LIMIT ?",
                    vectorStr, vectorStr, rag.getMinScore(), vectorStr, rag.getTopK());
        } catch (Exception e) {
            log.error("Admin KB 检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 关键词降级检索：按分词命中数排序，score 为伪分数（仅表示命中强度） */
    private List<Map<String, Object>> keywordSearch(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        var rag = config.getRag();
        List<Map<String, Object>> rows = vectorJdbcTemplate.queryForList(
                "SELECT title, content FROM admin_kb_chunks");
        // 简易中文分词：2-4 字滑窗 + 英文/数字词
        java.util.List<String> terms = extractTerms(query);
        List<Map<String, Object>> scored = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            String title = String.valueOf(row.get("title") == null ? "" : row.get("title"));
            String content = String.valueOf(row.get("content") == null ? "" : row.get("content"));
            int hits = 0;
            for (String term : terms) {
                if (content.contains(term) || title.contains(term)) {
                    hits++;
                }
            }
            if (hits > 0) {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("title", title);
                item.put("content", content);
                item.put("score", Math.min(0.95, 0.4 + 0.2 * hits));
                scored.add(item);
            }
        }
        scored.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return scored.stream().limit(rag.getTopK()).toList();
    }

    private java.util.List<String> extractTerms(String query) {
        java.util.List<String> terms = new java.util.ArrayList<>();
        String compact = query.replaceAll("[\s，。？！,.?!]", "");
        for (int len = 2; len <= 4; len++) {
            for (int i = 0; i + len <= compact.length(); i++) {
                String term = compact.substring(i, i + len);
                if (!terms.contains(term)) {
                    terms.add(term);
                }
            }
        }
        if (terms.size() > 24) {
            terms = terms.subList(0, 24);
        }
        return terms;
    }

    // ===== 分块：标题边界优先，超长滑窗 =====

    List<String> splitIntoChunks(String content, String title) {
        var rag = config.getRag();
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }
        // 按行收集，遇 ## / ### 边界开新块
        StringBuilder current = new StringBuilder();
        for (String line : content.split("\n")) {
            boolean heading = line.startsWith("## ") || line.startsWith("### ");
            if (heading && current.length() >= rag.getChunkSize() / 2) {
                addChunk(chunks, current.toString(), rag);
                current = new StringBuilder();
            }
            current.append(line).append('\n');
            if (current.length() >= rag.getChunkSize() * 2) {
                addChunk(chunks, current.toString(), rag);
                current = new StringBuilder();
            }
        }
        addChunk(chunks, current.toString(), rag);

        // 相邻块重叠（保留尾部 overlap 字符到下一块头部，简单实现：切片时上下文连续性靠标题分块保证，此处不再二次滑窗）
        return chunks.stream().map(c -> (title != null && !c.startsWith("【")) ? "【" + title + "】" + c : c)
                .collect(Collectors.toList());
    }

    private void addChunk(List<String> chunks, String text, AdminAgentConfig.Rag rag) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() <= rag.getChunkSize() * 2) {
            chunks.add(trimmed);
            return;
        }
        // 滑窗兜底（单段超长且无标题边界）
        for (int start = 0; start < trimmed.length(); start += rag.getChunkSize() - rag.getChunkOverlap()) {
            int end = Math.min(start + rag.getChunkSize() * 2, trimmed.length());
            chunks.add(trimmed.substring(start, end));
            if (end >= trimmed.length()) {
                break;
            }
        }
    }

    private String toVectorString(List<Double> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector.get(i));
        }
        return sb.append(']').toString();
    }

    // ===== 首次启动播种（运营知识内置条目，可直接被检索） =====

    private void seedIfEmpty() {
        if (docRepository.count() > 0) {
            return;
        }
        List<String[]> seeds = List.of(
                new String[]{"内容审核政策要点", "POLICY",
                        "## 文章审核\n通过条件：标题正文无敏感词、无违法信息、无恶意引流（联系方式+诱导话术）。\n拒绝（下架）：涉政敏感、色情低俗、暴力血腥、赌博诈骗、批量引流广告、侵犯版权。\n状态口径：status 2=已发布（通过），3=待审核，4=已下架（拒绝/下架）。\n## 评论审核\n通过→NORMAL；删除→DELETED（辱骂攻击、垃圾引流、刷屏）。\n## 举报处理\n举报状态 HANDLED=已处理 / DISMISSED=驳回 / PENDING=待处理。处理时必须填写处理说明。"},
                new String[]{"树洞审核政策要点", "POLICY",
                        "## 树洞帖\n审核通过→展示（status 1）；驳回→status 2。删除用于已上线内容的紧急处置。\n驳回标准：人身攻击、自残怂恿、违法信息、广告刷屏。\n树洞匿名身份由服务端加密签发，管理员无法反查真人；审核只针对内容。\n## 树洞举报\nresolve=处置有效，dismiss=举报无效驳回。处置前先核对原帖内容。"},
                new String[]{"数据口径说明", "METRIC",
                        "## 核心指标\n待审核文章=articles.status=3 的数量；待审核评论=comments.status=PENDING；待处理树洞=tree_hole_posts.status=0。\n## 仪表盘\ntotalUsers=注册总数；articleCount=已发布文章（status=2）；todayNew*=今日 00:00 起的新增。\n## 用户增长\ndailyNewUsers 按注册时间 created_at 分组统计；趋势=（今日-昨日)/昨日。"},
                new String[]{"运营操作口径", "MANUAL",
                        "## 推荐位\n首页推荐 FeaturedContent：targetType=ARTICLE 按文章挂载，position=HOME；重复添加同一文章会产生多条，先查再挂。\n## 反馈处理\n建议反馈 handle 后状态变 HANDLED 并可附回复；PENDING 为待处理。\n## 管理员操作\n所有 Agent 生成的写操作需管理员确认后执行；操作日志记录 AGENT_DRAFT/AGENT_EXEC/AGENT_CANCEL。"},
                new String[]{"运营处置执行规范", "POLICY",
                        "## 处置决策流程（五类操作通用）\n1. 取证：query_content_detail 查内容与举报；检索本规范。\n2. 分级：A级(事实清楚依据明确)→直接生成操作卡片，批量任务逐条生成(单次≤10)并输出处置汇总表；B级(边缘/证据不足/影响面大)→调用 ask_user 征询，给出建议倾向+理由+可选项，得到答复前不生成操作；C级(重置密码/角色变更/删文章)→无工具，说明手工步骤。\n3. 所有操作均为待确认卡片。\n## 评论审核\nA直接删除：辱骂攻击、色情赌博、纯广告引流、刷屏。B征询：吐槽批评无辱骂、敏感词但语义正常。A恢复：正常评论误标。\n## 举报处理\nA属实且明显违规→HANDLED+同步处置对象；A无违规事实→DISMISSED；B属实但轻微/疑似有组织→征询处置力度。\n## 树洞管理\nA拒绝/删除：违法信息、自残怂恿、赌博诈骗、人身攻击、广告刷屏。B征询：激烈宣泄无攻击对象、涉及具体个人(隐私/网暴风险)。涉自残倾向一律先征询并建议人工介入。A通过：正常情绪倾诉。匿名不可反查，处置只针对内容。\n## 推荐位\nA按管理员明确指令执行(撤下前确认在位)。B征询：助手主动建议推荐、撤高流量内容。A拒绝：待审/已下架文章不可推荐。\n## 反馈处理\nA已处理+指引：咨询类、产品建议(致谢已转产品)。B征询：Bug报告(口径)、投诉与账号纠纷(口径需把关)。"},
                new String[]{"平台功能速查", "MANUAL",
                        "## 纸域空间平台\nC 端功能：文章（AI 润色/一键生成/版本管理）、专栏、合集、语义搜索、知识库、知识星图、匿名树洞、互关私信（未互关单向仅 1 条）、成长签到、桌面萌宠、PWA。\n## AI 配额\nC 端 AI 功能每日共享 50 次；管理端助手每日 100 次，独立计算。"}
        );
        for (String[] seed : seeds) {
            AdminKbDoc doc = new AdminKbDoc();
            doc.setTitle(seed[0]);
            doc.setSourceType(seed[1]);
            doc.setContent(seed[2]);
            doc.setCreatedBy(null);
            doc = docRepository.save(doc);
            try {
                reindex(doc.getId());
            } catch (Exception e) {
                log.warn("种子文档索引失败: {}", doc.getTitle());
            }
        }
        log.info("Admin KB 已播种 {} 个内置知识文档", seeds.size());
    }
}
