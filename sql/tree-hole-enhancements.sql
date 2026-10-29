-- =====================================================
-- 匿名树洞功能增强 - 数据库迁移脚本
-- 适用环境: MySQL 8.0+
-- 日期: 2026-06-23
-- =====================================================

-- 1. 回复表新增共鸣(拥抱)计数
ALTER TABLE tree_hole_replies ADD COLUMN empathy_count INT DEFAULT 0;

-- 2. 回复表新增 AI 生成标识
ALTER TABLE tree_hole_replies ADD COLUMN is_ai_generated BOOLEAN DEFAULT FALSE;
