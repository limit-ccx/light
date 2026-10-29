-- ================================================
-- 纸域空间平台 - 运营管理数据种子脚本（第二部分）
-- ================================================
USE zhiguang;

-- ================================================
-- 9. 管理员操作日志（丰富的后台操作记录）
-- ================================================
INSERT INTO admin_logs (admin_id, action, target_type, target_id, detail, ip, created_at) VALUES
-- 用户管理操作
(1, 'BAN_USER', 'USER', 36, '{"username":"广告机器号","reason":"发布垃圾广告"}', '192.168.1.100', NOW() - INTERVAL 20 DAY),
(1, 'BAN_USER', 'USER', 37, '{"username":"违规账剠01","reason":"恶意刷量"}', '192.168.1.100', NOW() - INTERVAL 10 DAY),
(1, 'BAN_USER', 'USER', 38, '{"username":"恶意刷评号","reason":"发布违规评论"}', '192.168.1.100', NOW() - INTERVAL 3 DAY),
(1, 'UNBAN_USER', 'USER', 36, '{"username":"广告机器号","reason":"用户已申诉"}', '192.168.1.100', NOW() - INTERVAL 18 DAY),
(1, 'BAN_USER', 'USER', 36, '{"username":"广告机器号","reason":"再次发布广告"}', '192.168.1.100', NOW() - INTERVAL 15 DAY),
(1, 'SET_ROLE', 'USER', 12, '{"username":"林小溪","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 54 DAY),
(1, 'SET_ROLE', 'USER', 13, '{"username":"代码诗人","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 49 DAY),
(1, 'SET_ROLE', 'USER', 14, '{"username":"算法小王子","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 47 DAY),
(1, 'SET_ROLE', 'USER', 15, '{"username":"设计驱动","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 44 DAY),
(1, 'SET_ROLE', 'USER', 16, '{"username":"云原生探索者","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 39 DAY),
(1, 'SET_ROLE', 'USER', 17, '{"username":"数据库老司机","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 37 DAY),
(1, 'SET_ROLE', 'USER', 18, '{"username":"安全小卫士","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 34 DAY),
(1, 'SET_ROLE', 'USER', 19, '{"username":"移动端工匠","oldRole":"USER","newRole":"CREATOR"}', '192.168.1.100', NOW() - INTERVAL 31 DAY),
(1, 'RESET_PASSWORD', 'USER', 20, '{"username":"学习中的小白"}', '192.168.1.100', NOW() - INTERVAL 12 DAY),
-- 内容审核操作
(1, 'APPROVE_ARTICLE', 'ARTICLE', 13, '{"title":"React 19 正式发布"}', '192.168.1.100', NOW() - INTERVAL 53 DAY),
(1, 'APPROVE_ARTICLE', 'ARTICLE', 14, '{"title":"Go 1.22 泛型进阶"}', '192.168.1.100', NOW() - INTERVAL 49 DAY),
(1, 'APPROVE_ARTICLE', 'ARTICLE', 15, '{"title":"算法可视化"}', '192.168.1.100', NOW() - INTERVAL 46 DAY),
(1, 'APPROVE_ARTICLE', 'ARTICLE', 16, '{"title":"设计系统从 0 到 1"}', '192.168.1.100', NOW() - INTERVAL 43 DAY),
(1, 'APPROVE_ARTICLE', 'ARTICLE', 17, '{"title":"Kubernetes 生产环境踩坑"}', '192.168.1.100', NOW() - INTERVAL 38 DAY),
(1, 'APPROVE_ARTICLE', 'ARTICLE', 18, '{"title":"MySQL 8.0 性能调优"}', '192.168.1.100', NOW() - INTERVAL 36 DAY),
(1, 'REJECT_ARTICLE', 'ARTICLE', 39, '{"title":"震惊！这个方法竟然...","reason":"标题党"}', '192.168.1.100', NOW() - INTERVAL 6 DAY),
(1, 'TAKE_DOWN_ARTICLE', 'ARTICLE', 42, '{"title":"过时的技术文章","reason":"内容已过时"}', '192.168.1.100', NOW() - INTERVAL 28 DAY),
(1, 'APPROVE_COMMENT', 'COMMENT', 14, '{"content":"React Compiler 真的不需要 useMemo 了吗？"}', '192.168.1.100', NOW() - INTERVAL 52 DAY),
(1, 'APPROVE_COMMENT', 'COMMENT', 18, '{"content":"太赞了！终于看懂了快速排序"}', '192.168.1.100', NOW() - INTERVAL 45 DAY),
(1, 'DELETE_COMMENT', 'COMMENT', 38, '{"content":"这篇文章有抄袭嫌疑","reason":"经核实评论不实"}', '192.168.1.100', NOW() - INTERVAL 9 DAY),
(1, 'DELETE_COMMENT', 'COMMENT', 39, '{"content":"评论内容包含不当言论","reason":"包含敏感词"}', '192.168.1.100', NOW() - INTERVAL 4 DAY),
-- 推荐管理
(1, 'ADD_FEATURED', 'ARTICLE', 15, '{"title":"算法可视化","position":"HOME"}', '192.168.1.100', NOW() - INTERVAL 44 DAY),
(1, 'ADD_FEATURED', 'ARTICLE', 18, '{"title":"MySQL 8.0","position":"HOT_LIST"}', '192.168.1.100', NOW() - INTERVAL 34 DAY),
(1, 'ADD_FEATURED', 'ARTICLE', 24, '{"title":"AI 大模型 RAG","position":"EDITOR_PICK"}', '192.168.1.100', NOW() - INTERVAL 15 DAY),
(1, 'REMOVE_FEATURED', 'ARTICLE', 1, '{"title":"Vue 3 Composition API","reason":"推荐期到期"}', '192.168.1.100', NOW() - INTERVAL 5 DAY),
-- 举报处理
(1, 'HANDLE_REPORT', 'REPORT', 1, '{"result":"已处理，删除违规内容"}', '192.168.1.100', NOW() - INTERVAL 18 DAY),
(1, 'HANDLE_REPORT', 'REPORT', 3, '{"result":"举报不成立"}', '192.168.1.100', NOW() - INTERVAL 12 DAY),
(1, 'HANDLE_REPORT', 'REPORT', 5, '{"result":"已处理，封禁违规用户"}', '192.168.1.100', NOW() - INTERVAL 8 DAY),
-- 系统配置
(1, 'UPDATE_CONFIG', 'SYSTEM', NULL, '{"key":"content_review_enabled","old":"false","new":"true"}', '192.168.1.100', NOW() - INTERVAL 15 DAY),
(1, 'UPDATE_CONFIG', 'SYSTEM', NULL, '{"key":"sensitive_filter_enabled","old":"false","new":"true"}', '192.168.1.100', NOW() - INTERVAL 30 DAY),
(1, 'UPDATE_CONFIG', 'SYSTEM', NULL, '{"key":"hot_weight_likes","old":"3","new":"5"}', '192.168.1.100', NOW() - INTERVAL 20 DAY),
-- 敏感词管理
(1, 'ADD_SENSITIVE_WORD', 'SENSITIVE_WORD', NULL, '{"word":"赌博"}', '192.168.1.100', NOW() - INTERVAL 25 DAY),
(1, 'ADD_SENSITIVE_WORD', 'SENSITIVE_WORD', NULL, '{"word":"色情"}', '192.168.1.100', NOW() - INTERVAL 25 DAY),
-- Banner 管理
(1, 'ADD_BANNER', 'BANNER', 1, '{"title":"纸域空间平台 2.0 全新上线"}', '192.168.1.100', NOW() - INTERVAL 10 DAY),
(1, 'ADD_BANNER', 'BANNER', 2, '{"title":"创作者招募计划"}', '192.168.1.100', NOW() - INTERVAL 8 DAY),
(1, 'UPDATE_BANNER', 'BANNER', 1, '{"action":"更新排序"}', '192.168.1.100', NOW() - INTERVAL 5 DAY),
-- 公告管理
(1, 'PUBLISH_ANNOUNCEMENT', 'ANNOUNCEMENT', 1, '{"title":"平台升级公告 v2.0"}', '192.168.1.100', NOW() - INTERVAL 7 DAY),
(1, 'PUBLISH_ANNOUNCEMENT', 'ANNOUNCEMENT', 2, '{"title":"创作者激励计划启动"}', '192.168.1.100', NOW() - INTERVAL 3 DAY);

-- ================================================
-- 10. 举报数据
-- ================================================
INSERT INTO reports (reporter_id, target_type, target_id, reason, status, handler_id, handle_result, handled_at, created_at) VALUES
(20, 'COMMENT', 38, '评论内容不实，疑似恶意攻击', 'HANDLED', 1, '经核实评论不实，已删除该评论', NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 10 DAY),
(22, 'ARTICLE', 39, '标题党，内容与标题严重不符', 'HANDLED', 1, '已驳回文章，通知作者修改标题', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 7 DAY),
(24, 'ARTICLE', 40, '疑似广告推广内容', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 4 DAY),
(25, 'USER', 36, '该用户频繁发布垃圾内容', 'HANDLED', 1, '已对该用户进行封禁处理', NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 18 DAY),
(26, 'COMMENT', 39, '评论包含不当言论', 'HANDLED', 1, '已删除违规评论', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 5 DAY),
(12, 'ARTICLE', 40, '内容抄袭嫌疑', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 3 DAY),
(13, 'COMMENT', 39, '敏感词内容', 'HANDLED', 1, '已删除', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 4 DAY),
(20, 'USER', 37, '恶意刷量行为', 'HANDLED', 1, '封禁处理', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 12 DAY),
(14, 'ARTICLE', 41, '产品广告内容', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 1 DAY),
(22, 'USER', 38, '批量注册嫌疑', 'DISMISSED', 1, '经核实为正常用户', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 3 DAY),
(32, 'COMMENT', 14, '评论质量低下', 'DISMISSED', 1, '评论属于正常讨论', NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 9 DAY),
(25, 'ARTICLE', 39, '标题夸张不实', 'HANDLED', 1, '已通知作者修改', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 6 DAY);

-- ================================================
-- 11. 精选/推荐内容
-- ================================================
INSERT INTO featured_content (target_type, target_id, position, sort_order, curated_by, start_at, end_at, created_at) VALUES
('ARTICLE', 15, 'HOME', 100, 1, NOW() - INTERVAL 44 DAY, NOW() + INTERVAL 20 DAY, NOW() - INTERVAL 44 DAY),
('ARTICLE', 18, 'HOT_LIST', 95, 1, NOW() - INTERVAL 34 DAY, NOW() + INTERVAL 30 DAY, NOW() - INTERVAL 34 DAY),
('ARTICLE', 24, 'EDITOR_PICK', 90, 1, NOW() - INTERVAL 15 DAY, NOW() + INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY),
('ARTICLE', 13, 'HOME', 85, 1, NOW() - INTERVAL 20 DAY, NOW() + INTERVAL 10 DAY, NOW() - INTERVAL 20 DAY),
('ARTICLE', 16, 'HOT_LIST', 80, 1, NOW() - INTERVAL 10 DAY, NOW() + INTERVAL 20 DAY, NOW() - INTERVAL 10 DAY),
('COLUMN', 1, 'HOME', 75, 1, NOW() - INTERVAL 30 DAY, NOW() + INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY),
('COLUMN', 2, 'HOME', 70, 1, NOW() - INTERVAL 25 DAY, NOW() + INTERVAL 35 DAY, NOW() - INTERVAL 25 DAY),
('ARTICLE', 27, 'EDITOR_PICK', 88, 1, NOW() - INTERVAL 7 DAY, NOW() + INTERVAL 23 DAY, NOW() - INTERVAL 7 DAY);

-- ================================================
-- 12. 轮播图 Banner
-- ================================================
INSERT INTO banners (title, image_url, link_url, position, sort_order, status, created_at) VALUES
('纸域空间平台 2.0 全新上线', 'https://picsum.photos/seed/banner1/1200/400', '/about', 'HOME', 100, 'ACTIVE', NOW() - INTERVAL 10 DAY),
('创作者招募计划 - 加入纸域空间', 'https://picsum.photos/seed/banner2/1200/400', '/creator', 'HOME', 90, 'ACTIVE', NOW() - INTERVAL 8 DAY),
('前端技术专题 - 每周精选', 'https://picsum.photos/seed/banner3/1200/400', '/articles', 'HOME', 80, 'ACTIVE', NOW() - INTERVAL 5 DAY),
('AI 大模型应用开发专栏', 'https://picsum.photos/seed/banner4/1200/400', '/columns/4', 'HOME', 70, 'ACTIVE', NOW() - INTERVAL 3 DAY),
('旧版Banner-已禁用', 'https://picsum.photos/seed/banner5/1200/400', NULL, 'HOME', 50, 'DISABLED', NOW() - INTERVAL 60 DAY);

-- ================================================
-- 13. 平台公告
-- ================================================
INSERT INTO announcements (title, content, type, status, publish_at, created_at) VALUES
('平台升级公告 v2.0', '## 纸域空间平台 2.0 重大更新\n\n1. 全新运营后台：管理功能全面升级\n2. 创作者认证体系：新增创作者认证\n3. AI 智能助手：集成 AI 写作辅助功能\n4. 知识库系统：全新的个人知识管理功能', 'NOTICE', 'PUBLISHED', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY),
('创作者激励计划启动', '## 创作者激励计划\n\n- 认证创作者可获得更多曝光机会\n- 优质文章将进入精选推荐\n- 月度最佳创作者获得专属徽章', 'ACTIVITY', 'PUBLISHED', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
('系统维护通知', '## 系统维护通知\n\n计划于本周六凌晨 2:00-6:00 进行系统维护升级。', 'MAINTENANCE', 'DRAFT', NULL, NOW() - INTERVAL 1 DAY),
('关于内容审核规范更新的通知', '## 内容审核规范更新\n\n1. 标题党内容将被驳回\n2. 广告推广内容需要标注\n3. 转载内容需注明出处', 'NOTICE', 'PUBLISHED', NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY),
('春节活动预告', '## 春节特别活动\n\n纸域空间平台将举办春节技术征文活动，敬请期待！', 'ACTIVITY', 'ARCHIVED', NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 60 DAY);

-- ================================================
-- 14. 每日统计数据（最近60天）
-- ================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_daily_stats()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE stat_d DATE;
    WHILE i < 60 DO
        SET stat_d = DATE_SUB(CURDATE(), INTERVAL i DAY);
        INSERT IGNORE INTO daily_statistics (stat_date, metric, value, created_at) VALUES
            (stat_d, 'new_users', FLOOR(2 + RAND() * 8), NOW()),
            (stat_d, 'new_articles', FLOOR(1 + RAND() * 5), NOW()),
            (stat_d, 'new_comments', FLOOR(3 + RAND() * 12), NOW()),
            (stat_d, 'new_likes', FLOOR(5 + RAND() * 20), NOW()),
            (stat_d, 'active_users', FLOOR(20 + RAND() * 50), NOW()),
            (stat_d, 'page_views', FLOOR(100 + RAND() * 500), NOW()),
            (stat_d, 'ai_calls', FLOOR(10 + RAND() * 30), NOW());
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;
CALL generate_daily_stats();
DROP PROCEDURE IF EXISTS generate_daily_stats;

-- ================================================
-- 15. 创作者申请
-- ================================================
INSERT INTO creator_applications (user_id, direction, credentials, status, review_reason, reviewed_at, created_at) VALUES
(12, '前端开发', '5年前端开发经验，GitHub 1000+ stars', 'APPROVED', '资质符合要求', NOW() - INTERVAL 54 DAY, NOW() - INTERVAL 55 DAY),
(13, '后端开发', '10年Java开发经验，前阿里巴巴P7', 'APPROVED', '资质符合要求', NOW() - INTERVAL 49 DAY, NOW() - INTERVAL 50 DAY),
(14, '算法竞赛', 'ACM ICPC 金牌，Codeforces 2400+', 'APPROVED', '资质符合要求', NOW() - INTERVAL 47 DAY, NOW() - INTERVAL 48 DAY),
(15, 'UI设计', '前字节跳动设计师', 'APPROVED', '资质符合要求', NOW() - INTERVAL 44 DAY, NOW() - INTERVAL 45 DAY),
(16, '云原生', 'CKA/CKAD 认证', 'APPROVED', '资质符合要求', NOW() - INTERVAL 39 DAY, NOW() - INTERVAL 40 DAY),
(17, '数据库', '15年DBA经验', 'APPROVED', '资质符合要求', NOW() - INTERVAL 37 DAY, NOW() - INTERVAL 38 DAY),
(18, '网络安全', 'OSCP认证，多次CVE发现者', 'APPROVED', '资质符合要求', NOW() - INTERVAL 34 DAY, NOW() - INTERVAL 35 DAY),
(19, '移动开发', 'Flutter GDE，多个独立App', 'APPROVED', '资质符合要求', NOW() - INTERVAL 31 DAY, NOW() - INTERVAL 32 DAY),
(27, '游戏开发', '3年Unity开发经验', 'PENDING', NULL, NULL, NOW() - INTERVAL 5 DAY),
(35, '全栈开发', '自由职业者，5年全栈经验', 'PENDING', NULL, NULL, NOW() - INTERVAL 2 DAY),
(29, '内容创作', '自媒体运营经验', 'REJECTED', '资质不够充分', NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 18 DAY);

-- ================================================
-- 16. 站内消息
-- ================================================
INSERT INTO messages (sender_id, receiver_id, content, is_read, created_at) VALUES
(1, 12, '恭喜你的文章《React 19 正式发布》被评为编辑精选！', 1, NOW() - INTERVAL 15 DAY),
(1, 14, '你的算法可视化文章太棒了，已加入首页推荐！', 1, NOW() - INTERVAL 44 DAY),
(12, 1, '感谢推荐！会继续产出优质内容。', 1, NOW() - INTERVAL 14 DAY),
(20, 12, '大佬你好，React 19 的编译器原理能详细讲讲吗？', 0, NOW() - INTERVAL 10 DAY),
(22, 14, '请问排序算法的可视化代码开源了吗？', 0, NOW() - INTERVAL 8 DAY),
(1, 16, '欢迎成为纸域空间创作者！期待你的云原生系列文章。', 1, NOW() - INTERVAL 39 DAY),
(1, 17, 'MySQL 调优那篇文章质量很高，已加入热门列表。', 1, NOW() - INTERVAL 34 DAY),
(24, 18, '安全文章写得很实用，期待更多安全相关的内容！', 0, NOW() - INTERVAL 5 DAY),
(1, 19, 'Flutter 文章已加入编辑精选推荐。', 1, NOW() - INTERVAL 28 DAY),
(32, 13, 'Go 泛型文章对我帮助很大，感谢分享！', 0, NOW() - INTERVAL 3 DAY),
(1, 27, '你的创作者申请暂时未通过，请补充更多作品链接。', 1, NOW() - INTERVAL 15 DAY),
(35, 12, '想请教一下前端工程化的最佳实践。', 0, NOW() - INTERVAL 1 DAY);

-- ================================================
-- 17. 通知数据
-- ================================================
INSERT INTO notifications (user_id, type, content, article_id, is_read, created_at) VALUES
(12, 'SYSTEM', '你的文章被管理员推荐到首页精选', 13, 1, NOW() - INTERVAL 20 DAY),
(14, 'SYSTEM', '你的文章被管理员推荐到首页精选', 15, 1, NOW() - INTERVAL 44 DAY),
(12, 'LIKE', '用户「学习中的小白」赞了你的文章', 13, 1, NOW() - INTERVAL 52 DAY),
(12, 'COMMENT', '用户「学习中的小白」评论了你的文章', 13, 1, NOW() - INTERVAL 52 DAY),
(14, 'LIKE', '用户「大学生阿杰」赞了你的文章', 15, 1, NOW() - INTERVAL 45 DAY),
(17, 'COMMENT', '用户「运维小哥哥」评论了你的文章', 18, 0, NOW() - INTERVAL 32 DAY),
(16, 'FOLLOW', '用户「数据分析师」关注了你', NULL, 1, NOW() - INTERVAL 28 DAY),
(12, 'FOLLOW', '用户「在读研究生」关注了你', NULL, 0, NOW() - INTERVAL 15 DAY),
(18, 'LIKE', '你的文章《Web安全入门》获得了100个赞！', 19, 1, NOW() - INTERVAL 30 DAY),
(12, 'SYSTEM', '你的创作者认证已通过', NULL, 1, NOW() - INTERVAL 54 DAY),
(20, 'SYSTEM', '你关注的创作者「林小溪」发布了新文章', 13, 1, NOW() - INTERVAL 53 DAY),
(22, 'SYSTEM', '你关注的创作者「算法小王子」发布了新文章', 15, 1, NOW() - INTERVAL 46 DAY),
(24, 'COMMENT', '有人回复了你的评论', 13, 0, NOW() - INTERVAL 49 DAY),
(12, 'SYSTEM', '你有一篇文章有待审核的举报', 13, 0, NOW() - INTERVAL 10 DAY),
(27, 'SYSTEM', '你的创作者申请审核中，请耐心等待', NULL, 0, NOW() - INTERVAL 4 DAY),
(35, 'SYSTEM', '你的创作者申请审核中，请耐心等待', NULL, 0, NOW() - INTERVAL 1 DAY),
(29, 'SYSTEM', '你的创作者申请被驳回', NULL, 1, NOW() - INTERVAL 15 DAY),
(12, 'ACHIEVEMENT', '恭喜你获得成就「获得1000阅读」', NULL, 0, NOW() - INTERVAL 30 DAY),
(14, 'ACHIEVEMENT', '恭喜你获得成就「获得1000阅读」', NULL, 1, NOW() - INTERVAL 25 DAY),
(17, 'ACHIEVEMENT', '恭喜你获得成就「获得1000阅读」', NULL, 0, NOW() - INTERVAL 20 DAY),
(12, 'SYSTEM', '你的文章《Tailwind CSS v4》已发布成功', 25, 1, NOW() - INTERVAL 14 DAY),
(13, 'SYSTEM', '你的文章《Bun 1.0 实战》已发布成功', 26, 1, NOW() - INTERVAL 11 DAY),
(15, 'SYSTEM', '你的文章《Next.js 15 App Router》已发布成功', 27, 0, NOW() - INTERVAL 8 DAY),
(16, 'SYSTEM', '你的文章《微前端架构升级》已发布成功', 28, 0, NOW() - INTERVAL 5 DAY);

-- ================================================
-- 18. 用户等级与经验
-- ================================================
INSERT INTO user_levels (user_id, level, experience, created_at) VALUES
(12, 5, 2800, NOW() - INTERVAL 55 DAY),
(13, 5, 2650, NOW() - INTERVAL 50 DAY),
(14, 5, 2900, NOW() - INTERVAL 48 DAY),
(15, 4, 2100, NOW() - INTERVAL 45 DAY),
(16, 4, 2200, NOW() - INTERVAL 40 DAY),
(17, 5, 2750, NOW() - INTERVAL 38 DAY),
(18, 4, 1900, NOW() - INTERVAL 35 DAY),
(19, 3, 1500, NOW() - INTERVAL 32 DAY),
(20, 2, 450, NOW() - INTERVAL 52 DAY),
(22, 3, 1200, NOW() - INTERVAL 44 DAY),
(23, 2, 600, NOW() - INTERVAL 42 DAY),
(24, 2, 700, NOW() - INTERVAL 39 DAY),
(25, 2, 800, NOW() - INTERVAL 36 DAY),
(26, 2, 650, NOW() - INTERVAL 33 DAY),
(27, 1, 300, NOW() - INTERVAL 30 DAY),
(32, 1, 200, NOW() - INTERVAL 25 DAY),
(35, 1, 150, NOW() - INTERVAL 20 DAY)
ON DUPLICATE KEY UPDATE level = VALUES(level), experience = VALUES(experience);

-- ================================================
-- 19. 用户积分
-- ================================================
INSERT INTO user_points (user_id, points, total_points, created_at) VALUES
(12, 580, 1200, NOW() - INTERVAL 55 DAY),
(13, 520, 1100, NOW() - INTERVAL 50 DAY),
(14, 650, 1350, NOW() - INTERVAL 48 DAY),
(15, 480, 980, NOW() - INTERVAL 45 DAY),
(16, 510, 1050, NOW() - INTERVAL 40 DAY),
(17, 600, 1250, NOW() - INTERVAL 38 DAY),
(18, 420, 890, NOW() - INTERVAL 35 DAY),
(19, 350, 750, NOW() - INTERVAL 32 DAY),
(20, 120, 250, NOW() - INTERVAL 52 DAY),
(22, 280, 560, NOW() - INTERVAL 44 DAY),
(23, 150, 320, NOW() - INTERVAL 42 DAY),
(24, 180, 380, NOW() - INTERVAL 39 DAY),
(25, 200, 420, NOW() - INTERVAL 36 DAY),
(26, 160, 350, NOW() - INTERVAL 33 DAY),
(27, 80, 180, NOW() - INTERVAL 30 DAY)
ON DUPLICATE KEY UPDATE points = VALUES(points), total_points = VALUES(total_points);

-- ================================================
-- 20. 签到数据
-- ================================================
INSERT IGNORE INTO daily_sign_ins (user_id, sign_date, continuous_days, reward_points, created_at) VALUES
(12, CURDATE(), 7, 15, NOW()), (12, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 6, 12, NOW() - INTERVAL 1 DAY),
(12, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 5, 10, NOW() - INTERVAL 2 DAY), (12, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 4, 8, NOW() - INTERVAL 3 DAY),
(12, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 3, 6, NOW() - INTERVAL 4 DAY), (12, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 2, 4, NOW() - INTERVAL 5 DAY),
(12, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 1, 2, NOW() - INTERVAL 6 DAY),
(13, CURDATE(), 5, 10, NOW()), (13, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 4, 8, NOW() - INTERVAL 1 DAY),
(13, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 3, 6, NOW() - INTERVAL 2 DAY), (13, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2, 4, NOW() - INTERVAL 3 DAY),
(13, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 1, 2, NOW() - INTERVAL 4 DAY),
(14, CURDATE(), 3, 6, NOW()), (14, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 2, 4, NOW() - INTERVAL 1 DAY),
(14, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 1, 2, NOW() - INTERVAL 2 DAY),
(20, CURDATE(), 1, 2, NOW()),
(22, CURDATE(), 2, 4, NOW()), (22, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 2, NOW() - INTERVAL 1 DAY),
(25, CURDATE(), 1, 2, NOW()),
(32, CURDATE(), 1, 2, NOW());

-- ================================================
-- 21. 用户成就
-- ================================================
INSERT IGNORE INTO user_achievements (user_id, achievement_id, obtained_at) VALUES
(12, 1, NOW() - INTERVAL 53 DAY), (12, 4, NOW() - INTERVAL 30 DAY), (12, 5, NOW() - INTERVAL 25 DAY),
(13, 1, NOW() - INTERVAL 49 DAY), (13, 4, NOW() - INTERVAL 20 DAY), (13, 5, NOW() - INTERVAL 18 DAY),
(14, 1, NOW() - INTERVAL 46 DAY), (14, 4, NOW() - INTERVAL 15 DAY), (14, 5, NOW() - INTERVAL 12 DAY),
(15, 1, NOW() - INTERVAL 43 DAY), (15, 5, NOW() - INTERVAL 10 DAY),
(16, 1, NOW() - INTERVAL 38 DAY), (16, 5, NOW() - INTERVAL 8 DAY),
(17, 1, NOW() - INTERVAL 36 DAY), (17, 4, NOW() - INTERVAL 10 DAY), (17, 5, NOW() - INTERVAL 5 DAY),
(18, 1, NOW() - INTERVAL 33 DAY), (19, 1, NOW() - INTERVAL 30 DAY),
(12, 2, NOW() - INTERVAL 20 DAY), (13, 2, NOW() - INTERVAL 18 DAY), (14, 2, NOW() - INTERVAL 15 DAY);

-- ================================================
-- 补充 follows 表（FollowService 使用此表）
-- ================================================
INSERT IGNORE INTO follows (follower_id, following_id, created_at) VALUES
(20, 12, NOW() - INTERVAL 50 DAY), (20, 13, NOW() - INTERVAL 48 DAY), (20, 14, NOW() - INTERVAL 45 DAY),
(22, 12, NOW() - INTERVAL 40 DAY), (22, 15, NOW() - INTERVAL 38 DAY),
(24, 12, NOW() - INTERVAL 35 DAY), (24, 13, NOW() - INTERVAL 33 DAY), (24, 14, NOW() - INTERVAL 30 DAY),
(25, 17, NOW() - INTERVAL 30 DAY), (25, 18, NOW() - INTERVAL 28 DAY),
(26, 16, NOW() - INTERVAL 28 DAY), (26, 17, NOW() - INTERVAL 26 DAY),
(32, 12, NOW() - INTERVAL 25 DAY), (32, 13, NOW() - INTERVAL 24 DAY), (32, 14, NOW() - INTERVAL 23 DAY),
(35, 12, NOW() - INTERVAL 20 DAY), (35, 16, NOW() - INTERVAL 18 DAY),
(12, 13, NOW() - INTERVAL 48 DAY), (12, 14, NOW() - INTERVAL 45 DAY), (12, 15, NOW() - INTERVAL 42 DAY),
(13, 12, NOW() - INTERVAL 47 DAY), (13, 14, NOW() - INTERVAL 44 DAY),
(14, 12, NOW() - INTERVAL 44 DAY), (14, 13, NOW() - INTERVAL 43 DAY),
(15, 12, NOW() - INTERVAL 40 DAY), (16, 12, NOW() - INTERVAL 38 DAY), (16, 13, NOW() - INTERVAL 36 DAY),
(17, 16, NOW() - INTERVAL 34 DAY), (19, 12, NOW() - INTERVAL 28 DAY);

-- ================================================
-- 验证
-- ================================================
SELECT 'admin_logs' as 'table_name', COUNT(*) as 'cnt' FROM admin_logs
UNION ALL SELECT 'reports', COUNT(*) FROM reports
UNION ALL SELECT 'featured_content', COUNT(*) FROM featured_content
UNION ALL SELECT 'banners', COUNT(*) FROM banners
UNION ALL SELECT 'announcements', COUNT(*) FROM announcements
UNION ALL SELECT 'daily_statistics', COUNT(*) FROM daily_statistics
UNION ALL SELECT 'creator_applications', COUNT(*) FROM creator_applications
UNION ALL SELECT 'messages', COUNT(*) FROM messages
UNION ALL SELECT 'notifications', COUNT(*) FROM notifications;
