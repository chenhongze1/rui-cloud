# RUI Cloud Docker 环境配置

本目录包含了 RUI Cloud 框架所需的所有中间件的 Docker 配置文件，提供了完整的开发和测试环境。

## 📋 目录结构

```
docker/
├── mysql/
│   ├── conf/
│   │   └── my.cnf              # MySQL配置文件
│   └── init/
│       └── 01-init.sql         # 数据库初始化脚本
├── redis/
│   └── redis.conf              # Redis配置文件
├── nginx/
│   ├── nginx.conf              # Nginx主配置文件
│   └── conf.d/
│       └── rui-cloud.conf      # 应用反向代理配置
├── prometheus/
│   └── prometheus.yml          # Prometheus监控配置
└── grafana/
    └── provisioning/
        ├── datasources/
        │   └── prometheus.yml  # Grafana数据源配置
        └── dashboards/
            └── dashboard.yml   # 仪表板配置
```

## 🚀 快速开始

### 1. 启动基础服务

```bash
# 启动 MySQL 和 Redis
docker-compose up -d mysql redis
```

### 2. 启动完整监控环境

```bash
# 启动所有服务包括监控组件
docker-compose --profile monitoring up -d
```

### 3. 启动反向代理

```bash
# 启动 Nginx 反向代理
docker-compose --profile proxy up -d
```

### 4. 使用管理脚本

**Windows:**
```cmd
docker-start.bat
```

**Linux/Mac:**
```bash
./docker-start.sh
```

## 📊 服务访问地址

| 服务 | 地址 | 用户名/密码 | 说明 |
|------|------|-------------|------|
| MySQL | localhost:3306 | root/123456 | 数据库服务 |
| Redis | localhost:6379 | - | 缓存服务 |
| Jaeger UI | http://localhost:16686 | - | 链路追踪 |
| Zipkin UI | http://localhost:9411 | - | 链路追踪 |
| Prometheus | http://localhost:9090 | - | 监控指标 |
| Grafana | http://localhost:3000 | admin/admin123 | 可视化面板 |
| Nginx | http://localhost | - | 反向代理 |

## 🔧 配置说明

### MySQL 配置

- **字符集**: utf8mb4
- **时区**: Asia/Shanghai (+8:00)
- **最大连接数**: 200
- **缓冲池大小**: 256MB
- **慢查询日志**: 启用 (>2秒)

### Redis 配置

- **最大内存**: 256MB
- **内存策略**: allkeys-lru
- **持久化**: AOF + RDB
- **慢日志**: 启用 (>10ms)

### Nginx 配置

- **Gzip压缩**: 启用
- **客户端最大请求**: 50MB
- **代理超时**: 60秒
- **静态资源缓存**: 1年

## 📈 监控配置

### Prometheus 监控指标

- 应用指标: `/actuator/prometheus`
- JVM指标: `/actuator/metrics`
- 自定义指标: `/actuator/custom-metrics`
- 健康检查: `/actuator/health`

### Grafana 仪表板

预配置的数据源:
- Prometheus (指标数据)
- Jaeger (链路追踪)

## 🛠️ 常用命令

### 查看服务状态
```bash
docker-compose ps
```

### 查看服务日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 停止服务
```bash
# 停止所有服务
docker-compose --profile monitoring --profile proxy down

# 停止特定服务
docker-compose stop mysql
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart mysql
```

### 清理数据
```bash
# 停止服务并删除容器
docker-compose down

# 删除数据卷（注意：会丢失所有数据）
docker volume rm rui-cloud_mysql_data rui-cloud_redis_data
```

## 🔒 安全配置

### 生产环境建议

1. **修改默认密码**
   - MySQL root密码
   - Grafana admin密码
   - Redis密码（如需要）

2. **网络安全**
   - 限制端口访问
   - 配置防火墙规则
   - 使用内部网络

3. **SSL/TLS**
   - 配置HTTPS证书
   - 启用数据库SSL连接

## 🐛 故障排除

### 常见问题

1. **端口冲突**
   ```bash
   # 检查端口占用
   netstat -tulpn | grep :3306
   ```

2. **权限问题**
   ```bash
   # 确保Docker有足够权限
   sudo usermod -aG docker $USER
   ```

3. **内存不足**
   ```bash
   # 检查Docker资源限制
   docker system df
   docker system prune
   ```

4. **数据卷问题**
   ```bash
   # 检查数据卷
   docker volume ls
   docker volume inspect rui-cloud_mysql_data
   ```

### 日志位置

- MySQL错误日志: `/var/log/mysql/error.log`
- MySQL慢查询日志: `/var/log/mysql/slow.log`
- Nginx访问日志: `/var/log/nginx/access.log`
- Nginx错误日志: `/var/log/nginx/error.log`

## 📚 相关文档

- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [MySQL 8.0 文档](https://dev.mysql.com/doc/refman/8.0/en/)
- [Redis 配置文档](https://redis.io/topics/config)
- [Nginx 配置文档](http://nginx.org/en/docs/)
- [Prometheus 配置文档](https://prometheus.io/docs/prometheus/latest/configuration/configuration/)
- [Grafana 配置文档](https://grafana.com/docs/grafana/latest/)

## 🤝 贡献

如果您发现配置问题或有改进建议，请提交 Issue 或 Pull Request。

## 📄 许可证

本配置文件遵循项目的开源许可证。