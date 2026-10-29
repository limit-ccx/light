# 知光 (Zhiguang)

前后端分离的博客 / 社区平台，支持文章创作、社交互动、AI 辅助与后台治理。

## 功能特性

- **文章系统**：发布 / 编辑 / 草稿、标签、搜索、热门与推荐流
- **社交互动**：点赞、收藏、评论（支持回复与 @提及）、关注与动态 Feed
- **AI 能力**：AI 摘要、AI 导览助手、AI 润色与写作助手、AI 管理员助手（基于向量检索）
- **匿名树洞**：情绪标签、共情互动、AI 回复、内容审核
- **知识图谱**：文章知识点关联与可视化
- **后台管理**：用户管理、内容审核、举报处置、数据统计、权限分级
- **消息中心**：站内通知（RabbitMQ 异步投递）

## 技术栈

| 端 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Pinia + Element Plus |
| 后端 | Spring Boot 3.2 + Spring Security + JWT + MyBatis-Plus/JPA |
| 存储 | MySQL 8（主从）+ Redis + Elasticsearch + MinIO + PostgreSQL(pgvector) |
| 中间件 | RabbitMQ + Docker Compose |
| 可观测 | Prometheus + Grafana + Alertmanager + SkyWalking |

## 快速开始

```bash
# 1. 启动基础设施与应用（MySQL/Redis/ES/MQ/后端）
docker compose up -d

# 2. 前端
cd zhiguang-frontend
npm install
npm run dev
```

- 后端服务：http://localhost:8081
- 前端开发服务器：http://localhost:3000
- 数据库初始化脚本位于 `sql/`（自动挂载到 MySQL 容器首启执行）

## 目录结构

```
├── zhiguang-frontend/   # Vue 3 前端
├── zhiguang-backend/    # Spring Boot 后端
├── sql/                 # 建库与种子数据脚本
├── k8s/                 # Kubernetes 部署清单
├── prometheus/          # 告警规则与配置
└── docker-compose.yml   # 一键编排
```

## 默认账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 超级管理员 | admin | Admin@2024 |
