# Memes - AI 驱动的表情包社区平台

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.1-blue.svg)

基于 Spring Boot 3 的表情包分享平台，支持 AI 智能审核和自动生成犀利点评。

**本项目是基于原作者项目的二次开发版本**，主要改进：
- ✅ 从 Alibaba DashScope 迁移到 **OpenAI 兼容 API**
- ✅ 支持 **OpenAI**、**DeepSeek**、**Gemini** 等多种 AI 服务
- ✅ 支持自定义 OpenAI 兼容端点
- ✅ 更灵活的配置和更低的使用成本

---

## 🎯 项目特色

### AI 功能
- 🤖 **智能图片审核** - 使用 Vision API 自动审核上传的图片内容
- 💬 **AI 犀利点评** - 自动生成幽默风趣的图片点评
- 📊 **Token 使用监控** - 实时追踪 AI API 调用成本
- 🔄 **多模型支持** - 轻松切换不同 AI 服务提供商

### 社区功能
- 📱 **表情包分享** - 上传和分享有趣的表情包
- 🔍 **智能搜索** - 快速找到你想要的表情包
- 👥 **用户系统** - 完整的用户注册和管理
- 💾 **本地/云存储** - 灵活的文件存储方案

### MCP 协议支持
- 🚀 **实时更新** - 支持 SSE (Server-Sent Events) 实时推送
- 🔌 **MCP 端点** - https://mcp.bupt.site/sse
- 🛠️ **推荐客户端** - [Cherry Studio](https://github.com/CherryHQ/cherry-studio)

---

## 🚀 快速开始

### 先决条件

- Docker & Docker Compose
- OpenAI API Key (或 DeepSeek / 自定义端点)
- 域名（用于图片外网访问）

### 一键部署

1. **克隆仓库**
   ```bash
   git clone https://github.com/szemeng76/memes.git
   cd memes
   ```

2. **配置环境变量**

   编辑 `docker-compose.yml`，修改以下配置：
   ```yaml
   environment:
     # OpenAI API 配置
     - OPENAI_API_KEY=your_api_key_here          # 你的 API Key
     - OPENAI_BASE_URL=https://api.openai.com    # API 端点（不含 /v1）
     - OPENAI_MODEL=gpt-4o-mini                  # 使用的模型

     # 数据库密码
     - jdbcPassword=your_password

     # 应用 Token
     - token=your_app_token

     # 图片访问 URL 前缀
     - urlPrefix=https://your-domain.com/
   ```

3. **启动服务**
   ```bash
   docker compose up -d
   ```

4. **查看日志**
   ```bash
   docker compose logs -f memes-app
   ```

5. **访问应用**
   - 应用地址: `http://localhost:8081`
   - InfluxDB 监控: `http://localhost:8086`

---

## 🔧 配置选项

### 使用 OpenAI (官方)

```yaml
- OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxx
- OPENAI_BASE_URL=https://api.openai.com
- OPENAI_MODEL=gpt-4o-mini
```

**推荐模型**:
- `gpt-4o-mini` - 性价比最高
- `gpt-4o` - 能力最强

### 使用 DeepSeek (更便宜)

```yaml
- OPENAI_API_KEY=sk-xxxxxxxxxxxxx
- OPENAI_BASE_URL=https://api.deepseek.com
- OPENAI_MODEL=deepseek-chat
```

**优势**:
- 💰 价格约为 OpenAI 的 1/10
- 🚀 响应速度快
- 🌍 对海外服务器友好

### 使用自定义端点

```yaml
- OPENAI_API_KEY=your_api_key
- OPENAI_BASE_URL=https://api.yourdomain.com
- OPENAI_MODEL=gpt-4o-mini
```

**注意**: 不要在 Base URL 末尾添加 `/v1`，Spring AI 会自动添加。

---

## 📖 详细文档

- 📋 [OpenAI 迁移指南](OPENAI_MIGRATION.md) - 完整的迁移说明和配置指南
- 🔍 [故障排查指南](OPENAI_MIGRATION.md#-故障排查) - 常见问题解决方案
- ⚙️ [高级配置](OPENAI_MIGRATION.md#-高级配置) - 性能优化和自定义配置
- 📊 [监控指标](OPENAI_MIGRATION.md#-监控和指标) - Token 使用监控和成本估算

---

## 💰 成本对比

| 服务 | 输入价格 (每 1M tokens) | 输出价格 (每 1M tokens) | 推荐场景 |
|------|------------------------|------------------------|---------|
| **OpenAI GPT-4o-mini** | $0.15 | $0.60 | 生产环境，图片审核 |
| **OpenAI GPT-4o** | $2.50 | $10.00 | 复杂审核任务 |
| **DeepSeek** | $0.14 | $0.28 | 开发测试，成本优化 |

💡 **建议**: 开发/测试用 DeepSeek，生产环境用 GPT-4o-mini

---

## 🏗️ 技术栈

### 后端
- **Spring Boot 3.4.3** - 现代化的 Java 框架
- **Spring AI 1.0.1** - 统一的 AI 模型集成
- **MyBatis Plus** - 强大的持久层框架
- **Redis** - 缓存和会话管理
- **MySQL 8.0** - 关系型数据库

### 监控
- **Micrometer** - 应用指标收集
- **InfluxDB 2.7** - 时序数据库
- **自定义指标** - Token 使用、审核结果统计

### DevOps
- **Docker & Docker Compose** - 容器化部署
- **GitHub Actions** - CI/CD 自动化
- **GHCR** - 容器镜像托管

---

## 📦 项目结构

```
memes/
├── src/main/java/com/memes/
│   ├── controller/          # REST API 控制器
│   ├── service/             # 业务逻辑层
│   ├── schedule/            # 定时任务（AI 审核）
│   │   ├── AiReviewer.java  # 图片审核服务
│   │   └── SharpReview.java # 犀利点评服务
│   ├── model/               # 数据模型
│   └── util/                # 工具类
├── src/main/resources/
│   ├── application.yaml     # 主配置文件
│   ├── prompt.xml           # AI 审核提示词
│   └── sharp_review.xml     # 犀利点评提示词
├── docker-compose.yml       # Docker 编排文件
├── Dockerfile              # 镜像构建文件
├── pom.xml                 # Maven 依赖管理
├── README.md               # 本文件
└── OPENAI_MIGRATION.md     # OpenAI 迁移指南
```

---

## 🔨 本地开发

### 环境要求
- JDK 21+
- Maven 3.9+
- MySQL 8.0
- Redis 7+

### 编译项目
```bash
mvn clean package
```

### 运行测试
```bash
mvn test
```

### 本地运行
```bash
# 设置环境变量
export OPENAI_API_KEY=your_api_key
export OPENAI_BASE_URL=https://api.openai.com
export OPENAI_MODEL=gpt-4o-mini
export SPRING_PROFILES_ACTIVE=prod

# 运行应用
mvn spring-boot:run
```

---

## 🛠️ 常用命令

```bash
# 查看应用日志
docker compose logs -f memes-app

# 只看 AI 相关日志
docker compose logs -f memes-app | grep -i "openai\|review\|llm"

# 重启应用
docker compose restart memes-app

# 查看环境变量
docker compose exec memes-app env | grep OPENAI

# 进入容器调试
docker compose exec memes-app sh

# 清理并重新部署
docker compose down -v && docker compose up -d
```

---

## 📊 监控和指标

应用提供详细的 Token 使用监控：

- `total_token` - 总 Token 消耗
- `input_token` - 输入 Token 消耗
- `output_token` - 输出 Token 消耗
- `llm_review_count` - 审核结果统计
- `llm_api_error` - API 错误次数

访问 InfluxDB UI 查看详细指标: `http://localhost:8086`

---

## 🤝 参考和致谢

本项目基于原作者的开源项目进行二次开发：

**原项目信息**:
- 原作者创建了基础的表情包分享平台
- 提供了 MCP 协议支持和 SSE 实时推送功能
- 建立了完整的社区功能框架

**本项目改进**:
- ✅ 将 AI 部分从 Alibaba DashScope 迁移到 Spring AI + OpenAI
- ✅ 支持多种 AI 服务提供商（OpenAI、DeepSeek、自定义端点）
- ✅ 添加详细的 Token 使用监控和成本追踪
- ✅ 完善文档和部署指南
- ✅ 优化错误处理和重试机制

感谢原作者的开源贡献！

---

## 🚨 故障排查

### 问题 1: AI 审核不工作

**检查步骤**:
1. 确认 `SPRING_PROFILES_ACTIVE=prod`
2. 检查 API Key 是否正确
3. 查看日志中是否有 "Starting AI reviewer"

### 问题 2: Base URL 错误

**错误**: `Invalid URL (POST /v1/v1/chat/completions)`

**解决**: Base URL 不要包含 `/v1` 后缀
- ❌ `https://api.openai.com/v1`
- ✅ `https://api.openai.com`

### 问题 3: 图片超时

**错误**: `Download the media resource timed out`

**解决**:
- 确保 `urlPrefix` 配置正确
- 图片 URL 必须可以从公网访问
- 检查防火墙设置

更多问题请查看 [完整故障排查指南](OPENAI_MIGRATION.md#-故障排查)

---

## 📝 更新日志

### v2.0.0 (2025-11-03) - OpenAI 迁移版
- ✅ 完全移除 DashScope SDK
- ✅ 迁移到 Spring AI 1.0.1 + OpenAI
- ✅ 支持多种 AI 服务提供商
- ✅ 添加详细的监控和指标
- ✅ 完善文档和故障排查指南

### v1.0.0 (原版本)
- 基础表情包分享功能
- Alibaba DashScope AI 审核
- MCP 协议支持

---

## 📄 开源协议

本项目基于原项目进行二次开发，保留原项目的开源协议。

---

## 💬 联系和支持

- **MCP 端点**: https://mcp.bupt.site/sse
- **推荐客户端**: [Cherry Studio](https://github.com/CherryHQ/cherry-studio)
- **迁移文档**: [OPENAI_MIGRATION.md](OPENAI_MIGRATION.md)

如有问题，请查看文档或提交 Issue。

---

**⭐ 如果这个项目对你有帮助，欢迎 Star 支持！**

**Made with ❤️ by szemeng76**
**Based on the original open-source project**
