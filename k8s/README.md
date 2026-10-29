# 知光平台 Kubernetes 部署配置文件

此目录包含知光平台在 Kubernetes 集群中部署所需的所有配置文件。

## 目录结构

```
k8s/
├── namespace.yaml                   # 命名空间配置
├── configmap.yaml                   # 应用配置
├── secrets.yaml                     # 密钥配置
├── ai-service/
│   ├── deployment.yaml              # AI 服务部署
│   ├── service.yaml                 # AI 服务 Service
│   ├── hpa.yaml                     # AI 服务自动扩缩容
│   └── service-monitor.yaml         # Prometheus 监控配置
├── search-service/
│   ├── deployment.yaml              # 搜索服务部署
│   ├── service.yaml                 # 搜索服务 Service
│   ├── hpa.yaml                     # 搜索服务自动扩缩容
├── notification-service/
│   ├── deployment.yaml              # 通知服务部署
│   ├── service.yaml                 # 通知服务 Service
├── file-service/
│   ├── deployment.yaml              # 文件服务部署
│   ├── service.yaml                 # 文件服务 Service
├── backend-service/
│   ├── deployment.yaml              # 核心服务部署
│   ├── service.yaml                 # 核心服务 Service
├── frontend-service/
│   ├── deployment.yaml              # 前端服务部署
│   ├── service.yaml                 # 前端服务 Service
├── databases/
│   ├── mysql-statefulset.yaml       # MySQL 有状态服务
│   ├── redis-statefulset.yaml       # Redis 有状态服务
│   ├── rabbitmq-statefulset.yaml    # RabbitMQ 有状态服务
│   ├── elasticsearch-statefulset.yaml # Elasticsearch 有状态服务
│   ├── postgres-statefulset.yaml    # PostgreSQL 有状态服务
│   ├── minio-statefulset.yaml       # MinIO 有状态服务
├── ingress.yaml                     # Ingress 路由配置
├── monitoring/
│   ├── prometheus-deployment.yaml   # Prometheus 部署
│   ├── grafana-deployment.yaml      # Grafana 部署
│   ├── skywalking-oap.yaml          # SkyWalking OAP 部署
│   ├── skywalking-ui.yaml           # SkyWalking UI 部署
└── logging/
    ├── fluent-bit.yaml              # Fluent Bit 日志采集
```

## 使用步骤

1. 创建命名空间:
```bash
kubectl apply -f namespace.yaml
```

2. 创建配置和密钥:
```bash
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
```

3. 部署数据库服务:
```bash
kubectl apply -f databases/
```

4. 部署应用服务:
```bash
kubectl apply -f ai-service/
kubectl apply -f search-service/
kubectl apply -f notification-service/
kubectl apply -f file-service/
kubectl apply -f backend-service/
kubectl apply -f frontend-service/
```

5. 配置 Ingress:
```bash
kubectl apply -f ingress.yaml
```

6. 部署监控和日志:
```bash
kubectl apply -f monitoring/
kubectl apply -f logging/
```

## 验证部署

查看所有服务状态:
```bash
kubectl get all -n zhiguang
```

查看特定服务状态:
```bash
kubectl get pods -n zhiguang -l app=ai-service
kubectl logs -n zhiguang -l app=ai-service
```

查看服务日志:
```bash
kubectl logs -f deployment/ai-service -n zhiguang
```

进入 Pod 调试:
```bash
kubectl exec -it deployment/ai-service -n zhiguang -- /bin/sh
```

## 常用操作

扩容服务:
```bash
kubectl scale deployment ai-service --replicas=5 -n zhiguang
```

重启服务:
```bash
kubectl rollout restart deployment ai-service -n zhiguang
```

查看扩缩容状态:
```bash
kubectl get hpa -n zhiguang
```

查看 Ingress 状态:
```bash
kubectl get ingress -n zhiguang
```

## 注意事项

1. **镜像推送**: 部署前需将镜像推送到镜像仓库
2. **密钥管理**: secrets.yaml 中的密钥需 base64 编码
3. **资源调整**: 根据实际负载调整资源配置
4. **PV 配置**: 需配置 PersistentVolume 提供存储
5. **Ingress 配置**: 需配置域名和 SSL 证书

## 参考文档

- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [微服务拆分与 Kubernetes 部署方案](../知光平台微服务拆分与Kubernetes部署方案.md)