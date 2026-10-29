package com.zhiguang.service.agent;

import com.zhiguang.entity.AgentFeatureKb;
import com.zhiguang.repository.AgentFeatureKbRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 功能知识库：查询 + 首次启动种子数据。
 * <p>
 * 种子条目源自《知光客户端功能指南》（doc/知光客户端功能指南.md），
 * 上线后由运营通过管理端 CRUD 维护；条目内容不含页面路径（agent 不做导航）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentKbService {

    private final AgentFeatureKbRepository kbRepository;

    @PostConstruct
    public void seedIfEmpty() {
        try {
            if (kbRepository.count() > 0) {
                return;
            }
            kbRepository.saveAll(seedEntries());
            log.info("Agent 功能知识库已初始化 {} 条种子条目", seedEntries().size());
        } catch (Exception e) {
            log.warn("Agent 知识库种子初始化失败（表可能尚未创建）: {}", e.getMessage());
        }
    }

    /** 启用的知识条目（按排序） */
    public List<AgentFeatureKb> listEnabled() {
        return kbRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    /** 按关键词过滤（空关键词返回全量，最多 8 条） */
    public List<AgentFeatureKb> search(String keyword) {
        List<AgentFeatureKb> all = listEnabled();
        if (keyword == null || keyword.isBlank()) {
            return all.stream().limit(8).toList();
        }
        String kw = keyword.trim().toLowerCase();
        return all.stream()
                .filter(kb -> contains(kb.getTitle(), kw) || contains(kb.getSummary(), kw)
                        || contains(kb.getKeywords(), kw) || contains(kb.getDetail(), kw))
                .limit(8)
                .toList();
    }

    private boolean contains(String text, String kw) {
        return text != null && text.toLowerCase().contains(kw);
    }

    private List<AgentFeatureKb> seedEntries() {
        return Arrays.asList(
                kb("纸域空间是什么", 1, "纸域空间,知光,light note,paper domain,平台,介绍,社区",
                        "纸域空间是一个 AI 驱动的知识分享社区：写文章、沉淀知识、结识同好，提供 AI 写作辅助、专栏合集、知识库与知识星图、匿名树洞、好友私信、成长激励等一整套体验。"),
                kb("账号与登录", 2, "注册,登录,密码,锁定,账号,记住我",
                        "邮箱或用户名+密码注册登录，支持记住我。密码过于常见会被拒绝；短时间连续输错 5 次会临时锁定约 15 分钟，稍后再试即可。"),
                kb("发布文章", 3, "写文章,发布,草稿,创作,编辑器,标签,封面",
                        "在线编辑器撰写 Markdown 文章，可设置标题、摘要、标签、封面，支持草稿与发布两种状态。标题正文会经过内容安全校验；单篇约 1 万字符上限，超长建议拆分连载；被管理端下架的文章无法自行重新发布。"),
                kb("AI 润色", 4, "润色,ai,改写,扩写,精简,正式化,轻松化",
                        "AI 润色提供五种模式：智能润色、正式化、轻松化、精简提炼、扩写丰富。结果左右对比，支持差异高亮、按段落选择性采纳、一键撤销。AI 输出若因长文触顶会明确提示截断。"),
                kb("AI 写作助手（一键生成）", 5, "一键生成,标题,摘要,标签,ai写作,写作助手",
                        "基于文章正文一次生成标题候选（3-5 个）、摘要（80-150 字）、标签（3-8 个），也可分别单独生成；另有结构优化建议与自动纠错。"),
                kb("AI 每日配额", 6, "配额,次数,用完,限额,ai次数",
                        "AI 功能每日共享 50 次配额（润色、一键生成、纠错等共用），当日用完次日自动恢复。"),
                kb("版本管理", 7, "版本,历史,恢复,回滚,对比",
                        "文章的历史版本可查看、对比差异、一键恢复，误改不怕。"),
                kb("阅读与互动", 8, "点赞,收藏,评论,总结,阅读,互动",
                        "浏览文章流（推荐/热门）、点赞、收藏、评论与回复；文章附 AI 自动生成的总结，可快速把握要点。评论同样经过安全校验。"),
                kb("专栏", 9, "专栏,系列,连载,教程",
                        "围绕一个主题持续更新的系列内容，适合教程与连载。"),
                kb("合集", 10, "合集,收录,合集阅读",
                        "把相关文章收录成辑，方便读者成体系阅读。"),
                kb("搜索与语义搜索", 11, "搜索,找文章,语义,ai搜索",
                        "关键词全文搜索之外还有 AI 语义搜索：按意思而非字面匹配，用大白话描述也能找到内容相近的文章。"),
                kb("知识库", 12, "知识库,文档,导入,pdf,word",
                        "上传文档（如 PDF、Word），平台自动解析提炼，形成知识条目。"),
                kb("知识星图", 13, "知识图谱,星图,可视化,关系,星球",
                        "把知识条目之间的关系可视化为一片星海，可漫游探索星球之间的引力联结，直观看到知识脉络。"),
                kb("关注与私信", 14, "私信,聊天,好友,互关,关注,打招呼,发消息",
                        "关注感兴趣的创作者；未互关时每个方向只能发 1 条私信打个招呼（保护双方不被打扰）；互相关注即成为好友，好友间私信不限次数正常畅聊。单条上限 2000 字，内容经安全校验；删除仅对自己生效（对方仍可见，双方都删才彻底消失）；消息实时送达。"),
                kb("匿名树洞", 15, "树洞,匿名,情绪,心事,倾诉,共情",
                        "完全匿名的情绪树洞：按心情选情绪标签发帖，浏览他人树洞、送出共情反应、匿名回复、举报不当内容，夜间有专属氛围。匿名身份由服务端加密签发，任何人都无法从前端反查真人；发布有频率限制，内容审核通过后才展示。请温柔对待每一个匿名的心声。"),
                kb("成长与激励", 16, "签到,等级,成长,成就,头像商店,积分",
                        "每日签到领连续奖励；成长中心查看等级与成就徽章（如浏览量里程碑）；头像商店用成长积累兑换个性头像。"),
                kb("通知中心", 17, "通知,消息提醒,未读,公告",
                        "点赞、评论、关注、私信、系统公告等站内通知统一入口，未读数实时提醒。"),
                kb("创作者计划", 18, "创作者,申请,认证,权益",
                        "面向持续创作者的进阶身份，提交申请经平台审核通过后解锁更多创作权益（如专栏运营）。"),
                kb("个性化与陪伴", 19, "主题,暗色,亮色,宠物,桌宠,pwa,安装",
                        "亮色/暗色主题一键切换；桌面萌宠陪伴你逛社区；纸域空间支持安装到桌面/主屏（PWA），像原生应用一样打开。"),
                kb("帮助与反馈", 20, "帮助,反馈,联系,客服,faq,投诉,申诉",
                        "遇到问题可通过平台指南、常见问题、意见反馈、联系我们等渠道获得帮助；文章误判下架、账号问题等通过联系我们申诉。")
        );
    }

    private AgentFeatureKb kb(String title, int sort, String keywords, String detail) {
        AgentFeatureKb entity = new AgentFeatureKb();
        entity.setTitle(title);
        entity.setSummary(detail.split("。")[0] + "。");
        entity.setKeywords(keywords);
        entity.setDetail(detail);
        entity.setSortOrder(sort);
        entity.setEnabled(true);
        return entity;
    }
}
