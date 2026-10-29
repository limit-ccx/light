-- ================================================
-- 纸域空间平台 - 后台展示测试数据（100条）
-- 重点补充：树洞管理、内容审核
-- 可重复执行（INSERT IGNORE 规避唯一键冲突）
-- ================================================
USE zhiguang;

-- 供匿名数据生成唯一 hash
SET @salt = SHA2(CONCAT('seed', NOW(), RAND()), 256);

-- ================================================
-- 1. 树洞帖子 40 条
--    状态：1-已发布(25) 0-审核中(10) 2-已拒绝(5)
-- ================================================
SET @p0 = (SELECT COALESCE(MAX(id), 0) FROM tree_hole_posts);

INSERT INTO tree_hole_posts (anonymous_hash, content, emotion_tag, like_count, empathy_count, reply_count, view_count, report_count, status, score, created_at) VALUES
(SHA2(CONCAT(@salt,'p1'),256), '今天又被领导说了，明明不是我的错。回家路上看着路灯，突然觉得好累，不知道该跟谁说。', 'sadness', 12, 8, 3, 156, 0, 1, 45.2, NOW() - INTERVAL 25 DAY),
(SHA2(CONCAT(@salt,'p2'),256), '第一次一个人去看了场电影，反而觉得特别自由。原来孤独也可以是享受。', 'loneliness', 8, 15, 2, 98, 0, 1, 33.8, NOW() - INTERVAL 24 DAY),
(SHA2(CONCAT(@salt,'p3'),256), '暗恋三年的学长今天发朋友圈官宣了，哭了一整晚，但还是要说恭喜。', 'sadness', 21, 18, 4, 289, 0, 1, 78.5, NOW() - INTERVAL 23 DAY),
(SHA2(CONCAT(@salt,'p4'),256), '终于把拖欠了两个月的项目交付了！感觉自己又活过来了，奖励自己一顿火锅。', 'excitement', 34, 12, 5, 402, 0, 1, 96.3, NOW() - INTERVAL 22 DAY),
(SHA2(CONCAT(@salt,'p5'),256), '爸妈好像又吵架了。躲在自己房间里不敢出去，不知道该怎么办。', 'anxiety', 15, 20, 3, 176, 0, 1, 52.7, NOW() - INTERVAL 21 DAY),
(SHA2(CONCAT(@salt,'p6'),256), '公司优化裁员，名单里有我。三十岁失业，重新开始真的好难。', 'anxiety', 42, 30, 6, 511, 0, 1, 138.9, NOW() - INTERVAL 20 DAY),
(SHA2(CONCAT(@salt,'p7'),256), '捡到一只流浪猫，带回家养了。它缩在角落发抖的样子，像极了刚来这座城市的我。', 'happiness', 56, 25, 7, 623, 0, 1, 152.4, NOW() - INTERVAL 19 DAY),
(SHA2(CONCAT(@salt,'p8'),256), '室友半夜打游戏声音太大，说了三次都不听。真的想骂人。', 'anger', 9, 3, 1, 87, 0, 1, 28.6, NOW() - INTERVAL 18 DAY),
(SHA2(CONCAT(@salt,'p9'),256), '妈妈突然打电话来，就为了问我晚饭吃了没。挂了电话在公交上哭了。', 'gratitude', 67, 40, 8, 734, 0, 1, 189.2, NOW() - INTERVAL 17 DAY),
(SHA2(CONCAT(@salt,'p10'),256), '考研二战，每天泡在图书馆。不知道能不能考上，但不想放弃。', 'anxiety', 25, 16, 4, 312, 0, 1, 83.1, NOW() - INTERVAL 16 DAY),
(SHA2(CONCAT(@salt,'p11'),256), '和最好的朋友因为一件小事闹翻了。突然发现成年人的友谊真的很脆弱。', 'sadness', 18, 22, 5, 245, 0, 1, 68.4, NOW() - INTERVAL 15 DAY),
(SHA2(CONCAT(@salt,'p12'),256), '第一次自己做了一桌子菜请朋友来吃，他们说超好吃！成就感爆棚。', 'excitement', 29, 11, 3, 356, 0, 1, 88.7, NOW() - INTERVAL 14 DAY),
(SHA2(CONCAT(@salt,'p13'),256), '工作三年，存款还是零。月光族真的该醒醒了，从记账开始。', 'anxiety', 14, 9, 2, 167, 0, 1, 44.5, NOW() - INTERVAL 13 DAY),
(SHA2(CONCAT(@salt,'p14'),256), '深夜emo，翻遍了通讯录也找不到一个可以倾诉的人。', 'loneliness', 33, 28, 6, 421, 0, 1, 102.8, NOW() - INTERVAL 12 DAY),
(SHA2(CONCAT(@salt,'p15'),256), '今天被陌生人让座了，虽然我看起来可能确实很累。世界还是有温暖的。', 'gratitude', 22, 14, 2, 198, 0, 1, 58.3, NOW() - INTERVAL 11 DAY),
(SHA2(CONCAT(@salt,'p16'),256), '相亲对象上来就问房车存款，感觉像被估价一样。对爱情有点绝望。', 'anger', 17, 12, 4, 234, 0, 1, 62.9, NOW() - INTERVAL 10 DAY),
(SHA2(CONCAT(@salt,'p17'),256), '周末去爬山，山顶的风把烦恼都吹走了。大自然真的治愈。', 'happiness', 26, 9, 3, 287, 0, 1, 76.1, NOW() - INTERVAL 9 DAY),
(SHA2(CONCAT(@salt,'p18'),256), '发现妈妈的白头发又多了。时间过得真快，想多陪陪她。', 'sadness', 31, 19, 5, 342, 0, 1, 89.6, NOW() - INTERVAL 8 DAY),
(SHA2(CONCAT(@salt,'p19'),256), '终于拿到了心仪的offer！转行成功，这一年的努力没有白费。', 'excitement', 48, 21, 9, 567, 0, 1, 146.8, NOW() - INTERVAL 7 DAY),
(SHA2(CONCAT(@salt,'p20'),256), '奶奶住院了，在医院陪床。听着她平稳的呼吸声，才觉得安心。', 'anxiety', 19, 24, 4, 276, 0, 1, 71.5, NOW() - INTERVAL 6 DAY),
(SHA2(CONCAT(@salt,'p21'),256), '今天是我的生日，只有系统记得。一个人点了个小蛋糕，祝自己生日快乐。', 'loneliness', 58, 35, 7, 645, 0, 1, 162.7, NOW() - INTERVAL 5 DAY),
(SHA2(CONCAT(@salt,'p22'),256), '给爸妈买了新手机，他们嘴上说浪费，但偷偷跟亲戚炫耀。可爱。', 'gratitude', 39, 17, 4, 432, 0, 1, 112.3, NOW() - INTERVAL 4 DAY),
(SHA2(CONCAT(@salt,'p23'),256), '地铁上看到一个女生边哭边擦眼泪，很想递张纸巾，最后还是没敢。', 'sadness', 27, 26, 6, 389, 0, 1, 96.4, NOW() - INTERVAL 3 DAY),
(SHA2(CONCAT(@salt,'p24'),256), '减了两个月肥，终于瘦了十斤！裤子都松了，开心！', 'happiness', 23, 10, 3, 254, 0, 1, 68.9, NOW() - INTERVAL 2 DAY),
(SHA2(CONCAT(@salt,'p25'),256), '和异地恋的男友分手了。800公里的距离，终究还是输给了时间。', 'sadness', 45, 32, 8, 523, 0, 1, 134.6, NOW() - INTERVAL 1 DAY),
-- 审核中
(SHA2(CONCAT(@salt,'p26'),256), '怎么快速致富？求推荐一夜暴富的方法！', 'other', 0, 0, 0, 3, 0, 0, 0.5, NOW() - INTERVAL 8 HOUR),
(SHA2(CONCAT(@salt,'p27'),256), '有人要加微信吗？带你投资理财，稳赚不赔，私聊我！', 'other', 0, 0, 0, 2, 0, 0, 0.4, NOW() - INTERVAL 7 HOUR),
(SHA2(CONCAT(@salt,'p28'),256), '在线求一个能包吃包住包工作的对象，性别不限。', 'other', 1, 0, 0, 15, 0, 0, 3.2, NOW() - INTERVAL 6 HOUR),
(SHA2(CONCAT(@salt,'p29'),256), '分享一个绝对赚钱的副业，点击链接查看！', 'other', 0, 0, 0, 5, 0, 0, 0.9, NOW() - INTERVAL 5 HOUR),
(SHA2(CONCAT(@salt,'p30'),256), '今天好无聊，有没有人一起打游戏？', 'loneliness', 2, 1, 1, 28, 0, 0, 6.1, NOW() - INTERVAL 4 HOUR),
(SHA2(CONCAT(@salt,'p31'),256), '室友总偷用我的沐浴露，说了还不承认，好气。', 'anger', 3, 2, 1, 42, 0, 0, 9.8, NOW() - INTERVAL 3 HOUR),
(SHA2(CONCAT(@salt,'p32'),256), '要不要辞职去创业？纠结了一个月了。', 'anxiety', 4, 3, 2, 51, 0, 0, 12.4, NOW() - INTERVAL 2 HOUR),
(SHA2(CONCAT(@salt,'p33'),256), '第一次养绿植，叶子黄了，求各位大佬指点。', 'anxiety', 1, 1, 0, 19, 0, 0, 4.2, NOW() - INTERVAL 1 HOUR),
(SHA2(CONCAT(@salt,'p34'),256), '深夜突然很想哭，不知道为什么。', 'sadness', 5, 4, 1, 67, 0, 0, 15.3, NOW() - INTERVAL 30 MINUTE),
(SHA2(CONCAT(@salt,'p35'),256), '关注我，每天分享正能量语录！', 'other', 0, 0, 0, 8, 0, 0, 1.1, NOW() - INTERVAL 15 MINUTE),
-- 已拒绝
(SHA2(CONCAT(@salt,'p36'),256), '加V：xxxxx，各种资源免费送，懂的来。', 'other', 0, 0, 0, 0, 2, 2, 0.2, NOW() - INTERVAL 3 DAY),
(SHA2(CONCAT(@salt,'p37'),256), '震惊！转发这条消息可以领取红包！', 'other', 0, 0, 0, 0, 1, 2, 0.3, NOW() - INTERVAL 2 DAY),
(SHA2(CONCAT(@salt,'p38'),256), '这款保健品真的绝了，我已经吃了三个月，感觉年轻十岁！', 'other', 0, 0, 0, 0, 3, 2, 0.4, NOW() - INTERVAL 1 DAY),
(SHA2(CONCAT(@salt,'p39'),256), '讨厌的同学每天针对我，真想给他点颜色看看。', 'anger', 0, 0, 0, 0, 2, 2, 0.5, NOW() - INTERVAL 1 DAY),
(SHA2(CONCAT(@salt,'p40'),256), '借贷找我，利息低放款快，无抵押无担保。', 'other', 0, 0, 0, 0, 2, 2, 0.3, NOW() - INTERVAL 12 HOUR);

-- ================================================
-- 2. 树洞回复 25 条
--    状态：1-正常(20) 2-已拒绝(3) 3-已删除(2)
--    挂载到帖子 p1..p25（已发布帖）
-- ================================================
INSERT INTO tree_hole_replies (post_id, anonymous_hash, content, like_count, empathy_count, status, is_ai_generated, created_at) VALUES
(@p0+1, SHA2(CONCAT(@salt,'r1'),256), '抱抱你，错不在你，明天会更好的。', 6, 4, 1, 0, NOW() - INTERVAL 24 DAY),
(@p0+1, SHA2(CONCAT(@salt,'r2'),256), '我也被领导说过，后来发现他在PUA，早点离开这种环境吧。', 9, 3, 1, 0, NOW() - INTERVAL 23 DAY),
(@p0+2, SHA2(CONCAT(@salt,'r3'),256), '一个人的电影真的很爽，我经常这么干！', 4, 2, 1, 0, NOW() - INTERVAL 23 DAY),
(@p0+3, SHA2(CONCAT(@salt,'r4'),256), '抱抱你，爱而不得是常态，往前看。', 12, 6, 1, 0, NOW() - INTERVAL 22 DAY),
(@p0+4, SHA2(CONCAT(@salt,'r5'),256), '恭喜！火锅必须安排上！', 8, 1, 1, 0, NOW() - INTERVAL 21 DAY),
(@p0+5, SHA2(CONCAT(@salt,'r6'),256), '家家有本难念的经，找个机会好好聊聊吧。', 5, 3, 1, 0, NOW() - INTERVAL 20 DAY),
(@p0+6, SHA2(CONCAT(@salt,'r7'),256), '我也是今年被裁的，现在自由职业反而更开心。', 15, 8, 1, 0, NOW() - INTERVAL 19 DAY),
(@p0+7, SHA2(CONCAT(@salt,'r8'),256), '你拯救了一条小生命，善良的你一定也会被温柔以待。', 10, 6, 1, 0, NOW() - INTERVAL 18 DAY),
(@p0+8, SHA2(CONCAT(@salt,'r9'),256), '跟室友摊牌吧，沟通是唯一的解法。', 3, 1, 1, 0, NOW() - INTERVAL 17 DAY),
(@p0+9, SHA2(CONCAT(@salt,'r10'),256), '看哭了，妈妈的关心永远是最暖的。', 11, 5, 1, 0, NOW() - INTERVAL 16 DAY),
(@p0+10, SHA2(CONCAT(@salt,'r11'),256), '考研人加油！星光不负赶路人。', 7, 2, 1, 0, NOW() - INTERVAL 15 DAY),
(@p0+11, SHA2(CONCAT(@salt,'r12'),256), '如果还想挽回，勇敢一点；如果回不去，就放下。', 4, 2, 1, 0, NOW() - INTERVAL 14 DAY),
(@p0+12, SHA2(CONCAT(@salt,'r13'),256), '哇，听着就好吃，求菜谱！', 3, 1, 1, 0, NOW() - INTERVAL 13 DAY),
(@p0+14, SHA2(CONCAT(@salt,'r14'),256), '深夜的孤独我也懂，可以来这里多说说话。', 8, 4, 1, 0, NOW() - INTERVAL 12 DAY),
(@p0+15, SHA2(CONCAT(@salt,'r15'),256), '你也在给这个世界传递温暖呀！', 5, 2, 1, 0, NOW() - INTERVAL 11 DAY),
(@p0+17, SHA2(CONCAT(@salt,'r16'),256), '山顶的风吹走的不是烦恼，是你看待烦恼的视角。', 6, 3, 1, 0, NOW() - INTERVAL 9 DAY),
(@p0+18, SHA2(CONCAT(@salt,'r17'),256), '多回家看看，陪伴是最好的礼物。', 7, 3, 1, 0, NOW() - INTERVAL 8 DAY),
(@p0+19, SHA2(CONCAT(@salt,'r18'),256), '恭喜转行成功！了不起！', 9, 2, 1, 0, NOW() - INTERVAL 7 DAY),
(@p0+21, SHA2(CONCAT(@salt,'r19'),256), '生日快乐！虽然迟到了，但祝福不会迟到。', 14, 7, 1, 0, NOW() - INTERVAL 5 DAY),
(@p0+22, SHA2(CONCAT(@salt,'r20'),256), '爸妈的可爱从来都不让人失望。', 6, 2, 1, 0, NOW() - INTERVAL 4 DAY),
-- 已拒绝
(@p0+1, SHA2(CONCAT(@salt,'r21'),256), '加我微信，我有解药，包你开心。', 0, 0, 2, 0, NOW() - INTERVAL 10 HOUR),
(@p0+6, SHA2(CONCAT(@salt,'r22'),256), '我这有个稳赚的项目，私聊。', 0, 0, 2, 0, NOW() - INTERVAL 8 HOUR),
(@p0+7, SHA2(CONCAT(@salt,'r23'),256), '这只猫一看就是品种猫，转给我吧。', 0, 0, 2, 0, NOW() - INTERVAL 6 HOUR),
-- 已删除
(@p0+2, SHA2(CONCAT(@salt,'r24'),256), '（已删除的违规回复）', 0, 0, 3, 0, NOW() - INTERVAL 3 DAY),
(@p0+3, SHA2(CONCAT(@salt,'r25'),256), '（已删除的广告回复）', 0, 0, 3, 0, NOW() - INTERVAL 2 DAY);

-- ================================================
-- 3. 树洞举报 15 条
--    状态：0-待处理(8) 1-已处理(4) 2-已忽略(3)
-- ================================================
SET @r0 = (SELECT COALESCE(MAX(id), 0) FROM tree_hole_replies);

INSERT INTO tree_hole_reports (anonymous_hash, target_type, target_id, reason, status, created_at) VALUES
(SHA2(CONCAT(@salt,'rp1'),256), 'POST', @p0+26, '疑似广告引流', 0, NOW() - INTERVAL 8 HOUR),
(SHA2(CONCAT(@salt,'rp2'),256), 'POST', @p0+27, '涉嫌诈骗，诱导投资', 0, NOW() - INTERVAL 7 HOUR),
(SHA2(CONCAT(@salt,'rp3'),256), 'POST', @p0+29, '广告推广链接', 0, NOW() - INTERVAL 6 HOUR),
(SHA2(CONCAT(@salt,'rp4'),256), 'POST', @p0+35, '营销号引流', 0, NOW() - INTERVAL 5 HOUR),
(SHA2(CONCAT(@salt,'rp5'),256), 'REPLY', @r0+21, '广告骚扰', 0, NOW() - INTERVAL 10 HOUR),
(SHA2(CONCAT(@salt,'rp6'),256), 'REPLY', @r0+22, '涉嫌诈骗', 0, NOW() - INTERVAL 8 HOUR),
(SHA2(CONCAT(@salt,'rp7'),256), 'REPLY', @r0+23, '骚扰内容', 0, NOW() - INTERVAL 6 HOUR),
(SHA2(CONCAT(@salt,'rp8'),256), 'POST', @p0+38, '违规推广保健品', 0, NOW() - INTERVAL 2 HOUR),
(SHA2(CONCAT(@salt,'rp9'),256), 'POST', @p0+36, '广告内容，已处理', 1, NOW() - INTERVAL 3 DAY),
(SHA2(CONCAT(@salt,'rp10'),256), 'POST', @p0+37, '诱导转发，已处理', 1, NOW() - INTERVAL 2 DAY),
(SHA2(CONCAT(@salt,'rp11'),256), 'POST', @p0+40, '非法借贷广告，已处理', 1, NOW() - INTERVAL 12 HOUR),
(SHA2(CONCAT(@salt,'rp12'),256), 'POST', @p0+39, '攻击性言论，已处理', 1, NOW() - INTERVAL 1 DAY),
(SHA2(CONCAT(@salt,'rp13'),256), 'POST', @p0+30, '内容正常，忽略', 2, NOW() - INTERVAL 4 HOUR),
(SHA2(CONCAT(@salt,'rp14'),256), 'POST', @p0+32, '正常讨论，忽略', 2, NOW() - INTERVAL 3 HOUR),
(SHA2(CONCAT(@salt,'rp15'),256), 'REPLY', @r0+2, '正常回复，忽略', 2, NOW() - INTERVAL 5 HOUR);

-- ================================================
-- 4. 内容审核-待审核文章 10 条 (status=3)
-- ================================================
INSERT INTO articles (title, content, summary, user_id, status, view_count, like_count, favorite_count, comment_count, created_at) VALUES
('震惊！这个方法竟然能让工资翻倍', '<p>这是一篇标题党的文章，内容与标题严重不符，大量夸张描述。</p>', '震惊系列', 20, 3, 0, 0, 0, 0, NOW() - INTERVAL 3 DAY),
('免费领取价值999元的大礼包', '<p>文章中包含大量广告推广链接，诱导点击。</p>', '广告推广', 21, 3, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY),
('一夜暴富的真实经历分享', '<p>内容涉嫌虚假宣传，虚构赚钱经历，诱导添加微信。</p>', '虚假宣传', 22, 3, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY),
('你不知道的股票内幕消息', '<p>散布不实股市信息，涉嫌诱导投资。</p>', '投资风险', 23, 3, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY),
('这个保健品真的能治好你的病', '<p>文章宣传未经证实的产品功效，涉嫌违规医疗广告。</p>', '违规广告', 24, 3, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY),
('我的前端面试复盘（求求大佬指点）', '<p>分享了自己最近的前端面试经历，包括八股文、项目深挖和手写题，希望得到大家的建议。</p>', '面试经验', 25, 3, 0, 0, 0, 0, NOW() - INTERVAL 2 DAY),
('第一次写技术博客，关于Vue3的Composition API', '<p>记录自己学习Vue3 Composition API的过程，包含setup语法糖和响应式API的使用心得。</p>', 'Vue3学习', 26, 3, 0, 0, 0, 0, NOW() - INTERVAL 1 DAY),
('Java多线程并发编程入门', '<p>整理了Java多线程的基础知识，包括线程创建、同步、锁和线程池。</p>', 'Java并发', 27, 3, 0, 0, 0, 0, NOW() - INTERVAL 12 HOUR),
('Python爬虫实战：爬取天气数据', '<p>使用requests和BeautifulSoup实现了简单的天气数据爬取，附完整代码。</p>', 'Python爬虫', 28, 3, 0, 0, 0, 0, NOW() - INTERVAL 8 HOUR),
('搭建自己的个人博客网站', '<p>从零开始搭建个人博客，包括域名、服务器和静态博客框架的选择。</p>', '个人博客', 29, 3, 0, 0, 0, 0, NOW() - INTERVAL 4 HOUR);

-- ================================================
-- 5. 内容审核-评论 10 条（含待审核/违规）
-- ================================================
INSERT INTO comments (content, article_id, user_id, parent_id, status, created_at) VALUES
('这篇文章写得真好，学习了！', 1, 20, NULL, 'NORMAL', NOW() - INTERVAL 20 DAY),
('博主加油，期待更多干货！', 2, 21, NULL, 'NORMAL', NOW() - INTERVAL 18 DAY),
('加微信xxx，免费领资料！', 3, 22, NULL, 'PENDING', NOW() - INTERVAL 2 DAY),
('作者抄袭了我的文章！', 4, 23, NULL, 'PENDING', NOW() - INTERVAL 1 DAY),
('这个垃圾网站还能用吗？', 5, 24, NULL, 'PENDING', NOW() - INTERVAL 1 DAY),
('强烈推荐大家去看看这本书', 6, 25, NULL, 'NORMAL', NOW() - INTERVAL 15 DAY),
('学到很多，感谢分享~', 7, 26, NULL, 'NORMAL', NOW() - INTERVAL 12 DAY),
('同意楼上，说得太对了', 8, 27, 1, 'NORMAL', NOW() - INTERVAL 10 DAY),
('私聊我带你日赚500', 9, 28, NULL, 'PENDING', NOW() - INTERVAL 6 HOUR),
('路过，点个赞', 10, 29, NULL, 'NORMAL', NOW() - INTERVAL 3 DAY);

-- ================================================
-- 6. 内容举报 10 条
--    状态：PENDING(5) HANDLED(3) DISMISSED(2)
-- ================================================
INSERT INTO reports (reporter_id, target_type, target_id, reason, status, handler_id, handle_result, handled_at, created_at) VALUES
(20, 'ARTICLE', 45, '标题党，内容与标题严重不符', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 1 DAY),
(21, 'ARTICLE', 46, '涉嫌虚假宣传', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 12 HOUR),
(22, 'COMMENT', 41, '广告推广内容', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 8 HOUR),
(23, 'ARTICLE', 47, '诱导投资，散播不实信息', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 6 HOUR),
(24, 'COMMENT', 42, '辱骂作者，攻击性语言', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 4 HOUR),
(25, 'COMMENT', 43, '广告骚扰', 'HANDLED', 1, '已删除违规评论', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 DAY),
(26, 'ARTICLE', 48, '违规医疗广告', 'HANDLED', 1, '已驳回文章', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 2 DAY),
(27, 'COMMENT', 44, '刷屏广告', 'HANDLED', 1, '已封禁该用户', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 1 DAY),
(28, 'ARTICLE', 49, '内容质量低下', 'DISMISSED', 1, '内容属于正常讨论', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 DAY),
(29, 'COMMENT', 45, '正常回复', 'DISMISSED', 1, '评论属于正常讨论', NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 2 DAY);

-- ================================================
-- 统计确认
-- ================================================
SELECT 'tree_hole_posts' AS tbl, COUNT(*) AS cnt FROM tree_hole_posts
UNION ALL SELECT 'tree_hole_replies', COUNT(*) FROM tree_hole_replies
UNION ALL SELECT 'tree_hole_reports', COUNT(*) FROM tree_hole_reports
UNION ALL SELECT 'articles(待审核)', SUM(status=3) FROM articles
UNION ALL SELECT 'comments(含PENDING)', SUM(status='PENDING') FROM comments
UNION ALL SELECT 'reports', COUNT(*) FROM reports;

-- ================================================
-- 冗余计数校正：以明细表实际数量为准
-- （前面种子数据为 articles 写入了展示用假计数，
--   若不同步会导致文章详情页点赞/收藏/评论数与真实数据不符）
-- 本文件是初始化最后加载的脚本（04），此处统一校正全部文章
-- ================================================
UPDATE articles a SET
  a.like_count      = (SELECT COUNT(*) FROM article_likes l      WHERE l.article_id = a.id),
  a.favorite_count  = (SELECT COUNT(*) FROM article_favorites f  WHERE f.article_id = a.id),
  a.comment_count   = (SELECT COUNT(*) FROM comments c           WHERE c.article_id = a.id);
