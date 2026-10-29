-- ================================================
-- 纸域空间平台 - 运营数据种子脚本（追加式，不破坏现有数据）
-- ================================================
USE zhiguang;

-- ================================================
-- 1. 修复早期用户 NULL status
-- ================================================
UPDATE users SET status = 'NORMAL' WHERE status IS NULL;

-- ================================================
-- 2. 新增真实用户（密码统一 123456）
-- ================================================
-- BCrypt hash for "123456"
SET @pwd = '$2a$10$p1CLc10fKaBYcgTgftPjJOHe5hxgkYqXoQyeI8eMBIAN9rO012O6O';
SET @avatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';

INSERT INTO users (username, email, password, avatar, bio, role, status, created_at) VALUES
-- 创作者群体
('林小溪', 'linxiaoxi@qq.com', @pwd, @avatar, '前端开发工程师，热爱开源，专注 React 生态', 'CREATOR', 'NORMAL', NOW() - INTERVAL 55 DAY),
('代码诗人', 'codepoet@163.com', @pwd, @avatar, '用代码写诗，用算法画人生。Go 语言爱好者', 'CREATOR', 'NORMAL', NOW() - INTERVAL 50 DAY),
('算法小王子', 'algo@outlook.com', @pwd, @avatar, 'ACM 金牌选手，分享算法与数据结构', 'CREATOR', 'NORMAL', NOW() - INTERVAL 48 DAY),
('设计驱动', 'uxdesign@gmail.com', @pwd, @avatar, 'UI/UX 设计师，关注交互体验与设计系统', 'CREATOR', 'NORMAL', NOW() - INTERVAL 45 DAY),
('云原生探索者', 'cloudnative@qq.com', @pwd, @avatar, 'Kubernetes CKA 认证，专注云原生与微服务', 'CREATOR', 'NORMAL', NOW() - INTERVAL 40 DAY),
('数据库老司机', 'dbmaster@163.com', @pwd, @avatar, 'DBA 十五年，MySQL/PostgreSQL 性能调优专家', 'CREATOR', 'NORMAL', NOW() - INTERVAL 38 DAY),
('安全小卫士', 'secguard@outlook.com', @pwd, @avatar, '网络安全工程师，渗透测试与漏洞挖掘', 'CREATOR', 'NORMAL', NOW() - INTERVAL 35 DAY),
('移动端工匠', 'mobiledev@qq.com', @pwd, @avatar, 'Flutter/Swift 双栈开发，追求极致体验', 'CREATOR', 'NORMAL', NOW() - INTERVAL 32 DAY),
-- 普通用户群体
('学习中的小白', 'newbie001@qq.com', @pwd, @avatar, '正在学习编程的小白，希望每天进步一点点', 'USER', 'NORMAL', NOW() - INTERVAL 52 DAY),
('转行程序员', 'career_change@163.com', @pwd, @avatar, '从金融转行到IT，记录转行路上的点点滴滴', 'USER', 'NORMAL', NOW() - INTERVAL 47 DAY),
('大学生阿杰', 'ajie_stu@edu.cn', @pwd, @avatar, '计算机专业大三学生，准备秋招', 'USER', 'NORMAL', NOW() - INTERVAL 44 DAY),
('测试小姐姐', 'qa_girl@qq.com', @pwd, @avatar, '软件测试工程师，热爱自动化测试', 'USER', 'NORMAL', NOW() - INTERVAL 42 DAY),
('产品经理老王', 'pm_wang@163.com', @pwd, @avatar, '十年产品经验，懂技术的产品人', 'USER', 'NORMAL', NOW() - INTERVAL 39 DAY),
('运维小哥哥', 'devops_bro@outlook.com', @pwd, @avatar, 'Linux运维，Shell脚本爱好者', 'USER', 'NORMAL', NOW() - INTERVAL 36 DAY),
('数据分析师', 'data_analyst@qq.com', @pwd, @avatar, '数据驱动决策，Python/SQL 数据挖掘', 'USER', 'NORMAL', NOW() - INTERVAL 33 DAY),
('游戏开发者', 'gamedev@163.com', @pwd, @avatar, 'Unity/Unreal 游戏开发，分享游戏引擎技术', 'USER', 'NORMAL', NOW() - INTERVAL 30 DAY),
('嵌入式小哥', 'embed_dev@qq.com', @pwd, @avatar, '嵌入式系统开发，C/C++ 底层编程', 'USER', 'NORMAL', NOW() - INTERVAL 28 DAY),
('自由职业者', 'freelancer@outlook.com', @pwd, @avatar, '全栈自由职业，远程工作经验分享', 'USER', 'NORMAL', NOW() - INTERVAL 25 DAY),
('后端新人', 'backend_new@qq.com', @pwd, @avatar, 'Java后端初学者，求大佬带飞', 'USER', 'NORMAL', NOW() - INTERVAL 22 DAY),
('前端萌新', 'fe_newbie@163.com', @pwd, @avatar, 'Vue入门中，记录学习过程', 'USER', 'NORMAL', NOW() - INTERVAL 18 DAY),
('在读研究生', 'grad_student@edu.cn', @pwd, @avatar, 'NLP方向研究生，关注大语言模型', 'USER', 'NORMAL', NOW() - INTERVAL 15 DAY),
('创业CEO', 'startup_ceo@qq.com', @pwd, @avatar, '技术创业，SaaS产品从0到1', 'USER', 'NORMAL', NOW() - INTERVAL 12 DAY),
('独立开发者', 'indie_dev@outlook.com', @pwd, @avatar, '独立开发多个小产品，全栈技术爱好者', 'USER', 'NORMAL', NOW() - INTERVAL 8 DAY),
('退休老程序员', 'retired_coder@163.com', @pwd, @avatar, '写了三十年代码，现在纯粹享受编程乐趣', 'USER', 'NORMAL', NOW() - INTERVAL 5 DAY),
-- 被封禁用户
('广告机器号', 'spambot1@temp.com', @pwd, @avatar, NULL, 'USER', 'BANNED', NOW() - INTERVAL 20 DAY),
('违规账号01', 'violation01@temp.com', @pwd, @avatar, NULL, 'USER', 'BANNED', NOW() - INTERVAL 10 DAY),
('恶意刷评号', 'malicious01@temp.com', @pwd, @avatar, NULL, 'USER', 'BANNED', NOW() - INTERVAL 3 DAY);

-- ================================================
-- 3. 新增文章（分布在最近60天内，各种状态）
-- ================================================

-- 创作者文章（已发布，高质量）
INSERT INTO articles (title, content, summary, user_id, status, view_count, like_count, favorite_count, comment_count, created_at) VALUES
('React 19 正式发布：全新编译器与 Server Actions', '<h2>React 19 核心更新</h2><p>React 19 带来了全新的 React Compiler，可以自动优化组件的重新渲染，不再需要手动 useMemo 和 useCallback。Server Actions 也得到了全面增强，支持流式传输和乐观更新。</p><h2>新特性详解</h2><p>React Compiler 是基于 Rust 实现的，能够静态分析组件代码并自动注入优化逻辑。这意味着开发者可以专注于业务逻辑，而不必担心性能优化。</p><p>Server Actions 的改进包括：更好的错误处理、支持文件上传、与 React Query 的集成等。</p><h2>迁移指南</h2><p>从 React 18 升级到 19 是渐进式的，大部分代码无需修改。但需要注意一些 breaking changes。</p>', 'React 19 带来全新编译器与 Server Actions，深入解读核心变化', 12, 2, 3567, 234, 112, 45, NOW() - INTERVAL 53 DAY),

('Go 1.22 泛型进阶：类型约束的最佳实践', '<h2>泛型类型约束</h2><p>Go 1.22 引入了更强大的类型约束机制，包括联合类型约束和接口嵌入约束。本文通过实际案例展示如何在生产项目中合理使用泛型。</p><p>泛型不是银弹，需要权衡代码可读性和灵活性。</p><h2>实战案例</h2><p>我们来看一个通用的缓存实现，利用泛型让缓存支持任意类型的数据。</p>', 'Go 1.22 泛型进阶，通过实际案例展示类型约束的最佳实践', 13, 2, 2891, 178, 89, 32, NOW() - INTERVAL 49 DAY),

('算法可视化：十大排序算法动画演示', '<h2>为什么需要算法可视化</h2><p>学习算法最好的方式就是看到它的运行过程。本文用 JavaScript 实现了十大经典排序算法的动画演示，帮助你直观地理解每种算法的工作原理。</p><h2>实现的排序算法</h2><p>冒泡排序、选择排序、插入排序、归并排序、快速排序、堆排序、希尔排序、计数排序、桶排序、基数排序。</p>', '十大排序算法的动画可视化实现，直观理解算法原理', 14, 2, 5234, 389, 201, 67, NOW() - INTERVAL 46 DAY),

('设计系统从 0 到 1：Figma + React 组件库实践', '<h2>构建设计系统</h2><p>本文分享了从零构建设计系统的完整经验，包括 Design Token 定义、Figma 组件库搭建、React 组件库开发、文档站点部署等。</p><h2>技术选型</h2><p>我们选择了 Figma + Storybook + React + TypeScript 的技术栈，并使用 Changesets 管理版本发布。</p>', '从 Figma 到 React，完整的设计系统构建经验分享', 15, 2, 4102, 298, 156, 38, NOW() - INTERVAL 43 DAY),

('Kubernetes 生产环境踩坑记录（2024版）', '<h2>生产环境的血泪教训</h2><p>在将微服务迁移到 K8s 的过程中，我们遇到了很多意想不到的问题。本文记录了这些坑和对应的解决方案。</p><h2>典型问题</h2><p>Pod 频繁 OOMKilled、Service 负载均衡不均、PV 数据丢失、证书自动续签失败等。</p>', 'Kubernetes 生产环境的真实踩坑经验与解决方案', 16, 2, 3756, 245, 134, 52, NOW() - INTERVAL 38 DAY),

('MySQL 8.0 性能调优实战：从 5秒 降到 50毫秒', '<h2>慢查询优化</h2><p>一次真实的生产环境慢查询优化过程。通过 EXPLAIN 分析、索引优化、查询改写等手段，将一个 5秒 的查询优化到 50毫秒。</p><h2>优化步骤</h2><p>1. 使用慢查询日志定位问题 SQL；2. EXPLAIN 分析执行计划；3. 添加合适的索引；4. 改写查询语句；5. 验证优化效果。</p>', 'MySQL 慢查询优化的完整实战过程，百倍性能提升', 17, 2, 6789, 456, 234, 89, NOW() - INTERVAL 36 DAY),

('Web 安全入门：XSS/CSRF/SSRF 攻防详解', '<h2>Web 安全三板斧</h2><p>本文详细讲解了 Web 应用最常见的三种安全漏洞：XSS 跨站脚本攻击、CSRF 跨站请求伪造、SSRF 服务端请求伪造，以及对应的防御方案。</p>', 'XSS、CSRF、SSRF 三种常见 Web 安全漏洞的攻防实践', 18, 2, 4523, 312, 178, 41, NOW() - INTERVAL 33 DAY),

('Flutter 3.x 跨平台开发：一套代码征服全端', '<h2>Flutter 跨平台实战</h2><p>使用 Flutter 3.x 开发一个完整的跨平台应用，支持 iOS、Android、Web、Windows、macOS。分享实际项目中的架构设计和踩坑经验。</p>', 'Flutter 3.x 全平台开发实战经验分享', 19, 2, 3234, 213, 98, 29, NOW() - INTERVAL 30 DAY),

('2024 年前端工程化最佳实践', '<h2>工程化体系</h2><p>现代前端项目的工程化体系包括：代码规范（ESLint + Prettier）、Git 工作流（Git Flow）、CI/CD（GitHub Actions）、测试体系（Vitest + Playwright）等。</p>', '现代前端项目的完整工程化体系搭建指南', 12, 2, 2987, 201, 123, 35, NOW() - INTERVAL 27 DAY),

('分布式系统入门：CAP 理论与实际架构', '<h2>分布式基础</h2><p>从 CAP 理论出发，讲解分布式系统的设计原则，并结合 Redis Cluster、Kafka、ZooKeeper 等实际中间件进行分析。</p>', '分布式系统设计的核心理论与实践结合', 13, 2, 2456, 167, 87, 28, NOW() - INTERVAL 24 DAY),

('Rust 入门：为什么每个程序员都应该学 Rust', '<h2>Rust 的魅力</h2><p>Rust 连续多年成为 Stack Overflow 最受喜爱的编程语言。本文从零基础开始，介绍 Rust 的所有权系统、生命周期、并发安全等核心概念。</p>', 'Rust 编程语言核心概念入门指南', 14, 2, 3891, 278, 145, 43, NOW() - INTERVAL 20 DAY),

('AI 大模型应用开发：RAG 技术栈全面解析', '<h2>RAG 技术栈</h2><p>检索增强生成（RAG）是当前大模型应用最热门的技术方案。本文从向量数据库、Embedding 模型、检索策略、Prompt 工程等方面全面解析 RAG 技术栈。</p>', 'RAG 技术栈的完整解析与实践指南', 16, 2, 5678, 389, 234, 56, NOW() - INTERVAL 17 DAY),

('Tailwind CSS v4 重磅更新：Rust 引擎带来 10 倍性能', '<h2>Tailwind CSS v4</h2><p>Tailwind CSS v4 使用 Rust 重写了核心引擎，编译速度提升了 10 倍。同时还引入了全新的 CSS-first 配置方式和容器查询支持。</p>', 'Tailwind CSS v4 核心更新解读', 12, 2, 4123, 267, 134, 38, NOW() - INTERVAL 14 DAY),

('Bun 1.0 实战：能否替代 Node.js？', '<h2>Bun 运行时</h2><p>Bun 1.0 正式发布，号称是最快的 JavaScript 运行时。本文通过实际项目对比 Bun 和 Node.js 的性能表现。</p>', 'Bun 与 Node.js 性能对比实战测试', 13, 2, 2789, 189, 76, 24, NOW() - INTERVAL 11 DAY),

('Next.js 15 App Router 深度实践', '<h2>App Router 深度使用</h2><p>Next.js 15 的 App Router 已经非常成熟。本文分享在大型项目中使用 App Router 的最佳实践，包括 Server Components、Streaming、Parallel Routes 等。</p>', 'Next.js 15 App Router 在大型项目中的最佳实践', 15, 2, 3456, 234, 112, 36, NOW() - INTERVAL 8 DAY),

('微前端架构升级：从 qiankun 到 Module Federation', '<h2>微前端方案对比</h2><p>我们团队从 qiankun 迁移到 Webpack Module Federation 的完整过程。对比两种方案的优缺点，以及迁移中遇到的问题和解决方案。</p>', '微前端架构从 qiankun 迁移到 Module Federation 实战', 16, 2, 2890, 198, 98, 31, NOW() - INTERVAL 5 DAY),

('Redis 7.x 新特性与生产最佳实践', '<h2>Redis 7.x</h2><p>Redis 7.x 引入了 Redis Functions、Sharded Pub/Sub、Multi-part AOF 等新特性。本文结合实际生产场景介绍这些新特性的应用。</p>', 'Redis 7.x 新特性在生产环境中的应用', 17, 2, 2345, 156, 78, 22, NOW() - INTERVAL 3 DAY),

-- 普通用户文章（质量参差不齐）
('我的编程学习日记 Day 1', '<p>今天开始学习 Python，先搭环境，写了第一个 Hello World。</p>', '编程学习第一天', 20, 2, 123, 8, 2, 3, NOW() - INTERVAL 51 DAY),

('秋招面试经验总结', '<p>分享一下我秋招拿到的几个 offer 的面试经验，包括字节、阿里、腾讯的面试流程。</p>', '大厂面试经验分享', 22, 2, 1567, 89, 45, 18, NOW() - INTERVAL 41 DAY),

('Selenium 自动化测试入门教程', '<p>作为测试工程师，Selenium 是必备技能。本文带你从零开始学习 Selenium 自动化测试。</p>', 'Selenium 自动化测试入门', 23, 2, 876, 45, 23, 12, NOW() - INTERVAL 37 DAY),

('产品经理转技术的好处与坏处', '<p>作为一个有技术背景的产品经理，聊聊我的心得体会。</p>', '产品经理的技术之路', 24, 2, 654, 34, 12, 8, NOW() - INTERVAL 34 DAY),

('Linux 常用命令速查手册', '<p>整理了一份 Linux 运维常用的命令速查手册，方便随时查看。</p>', 'Linux 命令速查手册', 25, 2, 2345, 134, 67, 15, NOW() - INTERVAL 29 DAY),

('用 Python 做数据分析的入门指南', '<p>使用 Pandas + Matplotlib 进行数据分析的入门教程，附完整代码示例。</p>', 'Python 数据分析入门', 26, 2, 1890, 112, 56, 21, NOW() - INTERVAL 26 DAY),

('Unity 开发第一个 2D 小游戏', '<p>跟着教程做了第一个 Unity 2D 游戏，记录整个开发过程。</p>', 'Unity 2D 游戏开发初体验', 27, 2, 987, 56, 23, 9, NOW() - INTERVAL 23 DAY),

-- 草稿文章
('正在写的 React 教程...', '<p>还没写完...</p>', NULL, 12, 1, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY),
('Spring Cloud 微服务笔记', '<p>整理中...</p>', NULL, 13, 1, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY),

-- 待审核文章
('震惊！这个方法竟然...', '<p>这是一篇标题党的文章，内容包含一些夸张的描述...</p>', '震惊系列', 29, 3, 0, 0, 0, 0, NOW() - INTERVAL 7 DAY),
('免费赚钱的十个方法', '<p>本文介绍十种所谓的赚钱方法，内容有广告嫌疑...</p>', '赚钱方法', 30, 3, 0, 0, 0, 0, NOW() - INTERVAL 4 DAY),
('某个产品的推荐', '<p>推荐某个产品...</p>', '产品推荐', 31, 3, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY),

-- 已下架文章
('过时的技术文章', '<p>这篇文章的内容已经过时了...</p>', '已过时的内容', 20, 4, 234, 12, 5, 2, NOW() - INTERVAL 30 DAY);

-- ================================================
-- 4. 文章标签（为新增文章添加标签）
-- ================================================
INSERT INTO article_tags (article_id, tag) VALUES
(13, '前端'), (13, 'React'),
(14, '后端'), (14, 'Go'),
(15, '算法'), (15, '可视化'),
(16, '前端'), (16, '设计'), (16, 'React'),
(17, 'DevOps'), (17, 'Kubernetes'),
(18, '数据库'), (18, 'MySQL'), (18, '性能优化'),
(19, '安全'), (19, 'Web'),
(20, '移动端'), (20, 'Flutter'),
(21, '前端'), (21, '工程化'),
(22, '后端'), (22, '分布式'),
(23, 'Rust'), (23, '编程语言'),
(24, 'AI'), (24, '大模型'), (24, 'RAG'),
(25, '前端'), (25, 'CSS'),
(26, '后端'), (26, '运行时'),
(27, '前端'), (27, 'Next.js'),
(28, '前端'), (28, '微前端'),
(29, '后端'), (29, 'Redis'),
(30, 'Python'), (30, '入门'),
(31, '求职'), (31, '面试'),
(32, '测试'), (32, '自动化'),
(33, '产品'), (33, '思考'),
(34, '运维'), (34, 'Linux'),
(35, 'Python'), (35, '数据分析'),
(36, '游戏'), (36, 'Unity');

-- ================================================
-- 5. 评论数据（为各种文章添加丰富评论）
-- ================================================
INSERT INTO comments (content, article_id, user_id, status, created_at) VALUES
-- 文章13 (React 19)
('React Compiler 真的不需要 useMemo 了吗？感觉还是有点不放心', 13, 20, 'NORMAL', NOW() - INTERVAL 52 DAY),
('Server Actions 太方便了，省去了很多 API 层的代码', 13, 22, 'NORMAL', NOW() - INTERVAL 51 DAY),
('已经在项目中用上了，体验非常好！', 13, 15, 'NORMAL', NOW() - INTERVAL 50 DAY),
('性能提升很明显吗？有基准测试数据吗？', 13, 24, 'NORMAL', NOW() - INTERVAL 49 DAY),

-- 文章15 (算法可视化)
('太赞了！终于看懂了快速排序的分区过程', 15, 22, 'NORMAL', NOW() - INTERVAL 45 DAY),
('能加上红黑树的可视化吗？', 15, 26, 'NORMAL', NOW() - INTERVAL 44 DAY),
('收藏了，面试前必看！', 15, 32, 'NORMAL', NOW() - INTERVAL 43 DAY),
('归并排序的动画做得特别好，一看就懂', 15, 35, 'NORMAL', NOW() - INTERVAL 42 DAY),

-- 文章18 (MySQL 调优)
('这个优化过程太详细了，学到了很多', 18, 16, 'NORMAL', NOW() - INTERVAL 35 DAY),
('联合索引的最左前缀原则讲得很清楚', 18, 25, 'NORMAL', NOW() - INTERVAL 34 DAY),
('请问 EXPLAIN 里的 Extra 列怎么分析？', 18, 20, 'NORMAL', NOW() - INTERVAL 33 DAY),
('EXPLAIN 的 type 列从 ALL 变成 ref 就说明索引起效了', 18, 16, 'NORMAL', NOW() - INTERVAL 32 DAY),

-- 文章19 (Web安全)
('安全意识太重要了，感谢科普', 19, 24, 'NORMAL', NOW() - INTERVAL 32 DAY),
('XSS 的防御方案总结得很全面', 19, 12, 'NORMAL', NOW() - INTERVAL 31 DAY),

-- 文章24 (AI大模型RAG)
('RAG 是当前最实用的大模型应用方案了', 24, 12, 'NORMAL', NOW() - INTERVAL 16 DAY),
('向量数据库选型那块讲得很好', 24, 26, 'NORMAL', NOW() - INTERVAL 15 DAY),
('请问 Chunk 大小一般设多少合适？', 24, 32, 'NORMAL', NOW() - INTERVAL 14 DAY),

-- 文章31 (秋招面试)
('字节的面试流程确实是这样，我也经历过', 31, 20, 'NORMAL', NOW() - INTERVAL 40 DAY),
('恭喜拿到 offer！请问准备面试花了多长时间？', 31, 32, 'NORMAL', NOW() - INTERVAL 39 DAY),
('算法题刷了多久？LeetCode 刷了多少道？', 31, 22, 'NORMAL', NOW() - INTERVAL 38 DAY),

-- 文章34 (Linux命令)
('收藏了，比 man 好用多了', 34, 25, 'NORMAL', NOW() - INTERVAL 28 DAY),
('建议加上 systemctl 和 journalctl 相关命令', 34, 35, 'NORMAL', NOW() - INTERVAL 27 DAY),

-- 待审核文章的评论
('这个标题太夸张了吧', 37, 24, 'NORMAL', NOW() - INTERVAL 6 DAY),
('感觉像是广告', 38, 20, 'NORMAL', NOW() - INTERVAL 3 DAY),

-- 一些待审核评论
('这篇文章有抄袭嫌疑', 13, 29, 'PENDING', NOW() - INTERVAL 10 DAY),
('评论内容包含不当言论...', 18, 30, 'PENDING', NOW() - INTERVAL 5 DAY),
('正常评论测试', 24, 32, 'NORMAL', NOW() - INTERVAL 2 DAY);

-- ================================================
-- 6. 点赞数据
-- ================================================
INSERT IGNORE INTO article_likes (article_id, user_id, created_at) VALUES
(13, 20, NOW() - INTERVAL 52 DAY), (13, 22, NOW() - INTERVAL 51 DAY), (13, 24, NOW() - INTERVAL 50 DAY),
(15, 20, NOW() - INTERVAL 45 DAY), (15, 22, NOW() - INTERVAL 44 DAY), (15, 24, NOW() - INTERVAL 43 DAY), (15, 25, NOW() - INTERVAL 42 DAY),
(18, 12, NOW() - INTERVAL 35 DAY), (18, 13, NOW() - INTERVAL 34 DAY), (18, 14, NOW() - INTERVAL 33 DAY), (18, 15, NOW() - INTERVAL 32 DAY),
(19, 20, NOW() - INTERVAL 32 DAY), (19, 22, NOW() - INTERVAL 31 DAY),
(24, 12, NOW() - INTERVAL 16 DAY), (24, 13, NOW() - INTERVAL 15 DAY), (24, 14, NOW() - INTERVAL 14 DAY),
(31, 20, NOW() - INTERVAL 40 DAY), (31, 32, NOW() - INTERVAL 39 DAY),
(34, 25, NOW() - INTERVAL 28 DAY), (34, 35, NOW() - INTERVAL 27 DAY),
(14, 12, NOW() - INTERVAL 48 DAY), (14, 22, NOW() - INTERVAL 47 DAY),
(16, 12, NOW() - INTERVAL 42 DAY), (16, 13, NOW() - INTERVAL 41 DAY), (16, 20, NOW() - INTERVAL 40 DAY),
(17, 12, NOW() - INTERVAL 37 DAY), (17, 13, NOW() - INTERVAL 36 DAY), (17, 14, NOW() - INTERVAL 35 DAY),
(20, 12, NOW() - INTERVAL 26 DAY), (20, 13, NOW() - INTERVAL 25 DAY),
(21, 22, NOW() - INTERVAL 23 DAY), (21, 24, NOW() - INTERVAL 22 DAY),
(23, 12, NOW() - INTERVAL 19 DAY), (23, 13, NOW() - INTERVAL 18 DAY), (23, 14, NOW() - INTERVAL 17 DAY),
(25, 20, NOW() - INTERVAL 13 DAY), (25, 22, NOW() - INTERVAL 12 DAY),
(26, 12, NOW() - INTERVAL 10 DAY), (26, 22, NOW() - INTERVAL 9 DAY),
(27, 12, NOW() - INTERVAL 7 DAY), (27, 13, NOW() - INTERVAL 6 DAY),
(28, 12, NOW() - INTERVAL 4 DAY), (28, 13, NOW() - INTERVAL 3 DAY), (28, 14, NOW() - INTERVAL 2 DAY),
(29, 16, NOW() - INTERVAL 2 DAY), (29, 17, NOW() - INTERVAL 1 DAY);

-- ================================================
-- 7. 收藏数据
-- ================================================
INSERT IGNORE INTO article_favorites (article_id, user_id, created_at) VALUES
(13, 20, NOW() - INTERVAL 52 DAY), (13, 22, NOW() - INTERVAL 50 DAY),
(15, 20, NOW() - INTERVAL 45 DAY), (15, 22, NOW() - INTERVAL 44 DAY), (15, 24, NOW() - INTERVAL 43 DAY),
(18, 12, NOW() - INTERVAL 35 DAY), (18, 13, NOW() - INTERVAL 34 DAY), (18, 14, NOW() - INTERVAL 33 DAY),
(24, 12, NOW() - INTERVAL 16 DAY), (24, 13, NOW() - INTERVAL 15 DAY),
(31, 20, NOW() - INTERVAL 40 DAY), (31, 32, NOW() - INTERVAL 39 DAY),
(14, 12, NOW() - INTERVAL 48 DAY), (16, 12, NOW() - INTERVAL 42 DAY),
(17, 25, NOW() - INTERVAL 37 DAY), (19, 20, NOW() - INTERVAL 32 DAY),
(23, 12, NOW() - INTERVAL 19 DAY), (23, 13, NOW() - INTERVAL 18 DAY),
(25, 20, NOW() - INTERVAL 13 DAY), (27, 12, NOW() - INTERVAL 7 DAY);

-- ================================================
-- 8. 关注关系
-- ================================================
INSERT IGNORE INTO user_follows (follower_id, following_id, created_at) VALUES
-- 用户关注创作者
(20, 12, NOW() - INTERVAL 50 DAY), (20, 13, NOW() - INTERVAL 48 DAY), (20, 14, NOW() - INTERVAL 45 DAY),
(22, 12, NOW() - INTERVAL 40 DAY), (22, 15, NOW() - INTERVAL 38 DAY),
(24, 12, NOW() - INTERVAL 35 DAY), (24, 13, NOW() - INTERVAL 33 DAY), (24, 14, NOW() - INTERVAL 30 DAY),
(25, 17, NOW() - INTERVAL 30 DAY), (25, 18, NOW() - INTERVAL 28 DAY),
(26, 16, NOW() - INTERVAL 28 DAY), (26, 17, NOW() - INTERVAL 26 DAY),
(32, 12, NOW() - INTERVAL 25 DAY), (32, 13, NOW() - INTERVAL 24 DAY), (32, 14, NOW() - INTERVAL 23 DAY),
(35, 12, NOW() - INTERVAL 20 DAY), (35, 16, NOW() - INTERVAL 18 DAY),
-- 创作者互相关注
(12, 13, NOW() - INTERVAL 48 DAY), (12, 14, NOW() - INTERVAL 45 DAY), (12, 15, NOW() - INTERVAL 42 DAY),
(13, 12, NOW() - INTERVAL 47 DAY), (13, 14, NOW() - INTERVAL 44 DAY),
(14, 12, NOW() - INTERVAL 44 DAY), (14, 13, NOW() - INTERVAL 43 DAY),
(15, 12, NOW() - INTERVAL 40 DAY), (16, 12, NOW() - INTERVAL 38 DAY), (16, 13, NOW() - INTERVAL 36 DAY),
(17, 16, NOW() - INTERVAL 34 DAY), (18, 17, NOW() - INTERVAL 32 DAY), (18, 18, NOW() - INTERVAL 30 DAY),
(19, 12, NOW() - INTERVAL 28 DAY);
