# 🐳 RUI Cloud Docker 环境配置指南

本文档提供了 RUI Cloud 框架的完整 Docker 环境配置，包括所有必需的中间件和可选的监控组件。

## 📦 包含的服务

### 🔧 基础服务
- **MySQL 8.0** - 主数据库
- **Redis 7** - 缓存和会话存储

### 📊 监控服务（可选）
- **Jaeger** - 分布式链路追踪
- **Zipkin** - 链路追踪（备选）
- **Prometheus** - 指标收集和监控
- **Grafana** - 数据可视化和仪表板

### 🌐 代理服务（可选）
- **Nginx** - 反向代理和负载均衡

## 🚀 快速开始

### 1. 环境准备

确保已安装以下软件：
- Docker Desktop (Windows/Mac) 或 Docker Engine (Linux)
- Docker Compose v2.0+

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 根据需要编辑配置
# 编辑 .env 文件中的配置项
```

### 3. 启动服务

#### 方式一：使用管理脚本（推荐）

**Windows:**
```cmd
# 双击运行或在命令行执行
docker-start.bat
```

**Linux/Mac:**
```bash
# 添加执行权限
chmod +x docker-start.sh

# 运行脚本
./docker-start.sh
```

#### 方式二：直接使用 Docker Compose

```bash
# 启动基础服务
docker-compose up -d mysql redis

# 启动完整监控环境
docker-compose --profile monitoring up -d

# 启动反向代理
docker-compose --profile proxy up -d
```

## 🔗 服务访问

启动成功后，可以通过以下地址访问各个服务：

| 服务 | 访问地址 | 认证信息 |
|------|----------|----------|
| 应用主页 | http://localhost:8080 | - |
| MySQL | localhost:3306 | root / 123456 |
| Redis | localhost:6379 | 无密码 |
| Nginx代理 | http://localhost | - |
| Jaeger UI | http://localhost:16686 | - |
| Zipkin UI | http://localhost:9411 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin / admin123 |

## 📋 管理脚本功能

管理脚本提供以下功能：

1. **启动基础服务** - 仅启动 MySQL 和 Redis
2. **启动完整服务** - 启动所有服务包括监控组件
3. **启动代理服务** - 启动 Nginx 反向代理
4. **停止所有服务** - 停止所有运行的容器
5. **重启服务** - 重启所有服务
6. **查看服务状态** - 显示容器运行状态
7. **查看服务日志** - 实时查看服务日志
8. **清理数据卷** - 删除所有持久化数据

## 🔧 配置自定义

### 修改端口

编辑 `.env` 文件中的端口配置：

```env
MYSQL_PORT=3306
REDIS_PORT=6379
NGINX_HTTP_PORT=80
GRAFANA_PORT=3000
# ... 其他端口配置
```

### 修改密码

```env
MYSQL_ROOT_PASSWORD=your_secure_password
GRAFANA_ADMIN_PASSWORD=your_grafana_password
```

### 自定义数据卷路径

```env
MYSQL_DATA_PATH=./data/mysql
REDIS_DATA_PATH=./data/redis
```

## 📊 监控配置

### Prometheus 指标收集

应用需要暴露以下端点：
- `/actuator/prometheus` - Prometheus 格式指标
- `/actuator/health` - 健康检查
- `/actuator/metrics` - 应用指标

### Grafana 仪表板

预配置的数据源：
- **Prometheus** - 指标数据源
- **Jaeger** - 链路追踪数据源

### 链路追踪配置

在应用配置中启用链路追踪：

```yaml
rui:
  tracing:
    enabled: true
    jaeger:
      enabled: true
      endpoint: http://localhost:14268/api/traces
```

## 🛠️ 故障排除

### 常见问题

#### 1. 端口冲突
```bash
# 检查端口占用
netstat -tulpn | grep :3306

# 修改 .env 文件中的端口配置
```

#### 2. 容器启动失败
```bash
# 查看容器日志
docker-compose logs mysql

# 检查容器状态
docker-compose ps
```

#### 3. 数据库连接失败
```bash
# 确保 MySQL 容器正在运行
docker-compose ps mysql

# 检查 MySQL 日志
docker-compose logs mysql

# 测试连接
docker-compose exec mysql mysql -uroot -p123456
```

#### 4. 内存不足
```bash
# 检查 Docker 资源使用
docker system df

# 清理未使用的资源
docker system prune
```

### 重置环境

如果遇到严重问题，可以完全重置环境：

```bash
# 停止所有服务
docker-compose --profile monitoring --profile proxy down

# 删除所有容器和网络
docker-compose --profile monitoring --profile proxy down --volumes --remove-orphans

# 删除数据卷（注意：会丢失所有数据）
docker volume prune

# 重新启动
docker-compose up -d
```

## 🔒 生产环境部署

### 安全配置

1. **修改默认密码**
   ```env
   MYSQL_ROOT_PASSWORD=strong_password_here
   GRAFANA_ADMIN_PASSWORD=strong_password_here
   ```

2. **启用 Redis 密码**
   ```env
   REDIS_PASSWORD=redis_password_here
   ```

3. **配置 SSL/TLS**
   - 将 SSL 证书放在 `docker/nginx/ssl/` 目录
   - 修改 Nginx 配置启用 HTTPS

### 性能优化

1. **调整 MySQL 配置**
   ```env
   MYSQL_INNODB_BUFFER_POOL_SIZE=1G
   MYSQL_MAX_CONNECTIONS=500
   ```

2. **调整 Redis 配置**
   ```env
   REDIS_MAX_MEMORY=1gb
   ```

3. **调整 Nginx 配置**
   ```env
   NGINX_WORKER_PROCESSES=4
   NGINX_WORKER_CONNECTIONS=2048
   ```

## 📚 相关文档

- [Docker 配置详细说明](./docker/README.md)
- [应用配置指南](./README.md)
- [性能监控迁移指南](./rui-common/rui-common-monitoring/PERFORMANCE_MIGRATION_GUIDE.md)

## 🤝 支持

如果遇到问题或有改进建议：

1. 查看 [故障排除](#-故障排除) 部分
2. 检查 [Issues](https://github.com/your-repo/issues)
3. 提交新的 Issue 或 Pull Request

---

**注意**: 本配置主要用于开发和测试环境。生产环境部署时请根据实际需求调整配置并加强安全措施。