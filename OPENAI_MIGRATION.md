# OpenAI API 迁移指南

## 🎉 迁移完成！

已成功将 DashScope SDK 替换为 OpenAI 兼容 API，现在支持：
- ✅ **OpenAI** (GPT-4o, GPT-4o-mini, etc.)
- ✅ **DeepSeek** (deepseek-chat, deepseek-reasoner)
- ✅ **Gemini** (通过 OpenAI 兼容接口)
- ✅ **其他任何 OpenAI 兼容的 API**

---

## 📋 变更内容

### 1. 依赖更新 (pom.xml)
```xml
<!-- 移除 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
</dependency>

<!-- 添加 Spring AI OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2. 配置更新 (application.yaml)
```yaml
# 旧配置
dashscope:
  apiKey: ${dashscopeApiKey}

# 新配置 (Spring AI)
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
          temperature: 0.0
```

### 3. 代码重构
- `AiReviewer.java` - 使用 Spring AI OpenAI Vision API 进行图片审核
- `SharpReview.java` - 使用 Spring AI OpenAI Chat API 进行文本生成
- 两个类都通过依赖注入 `ChatModel` 来调用 AI 服务

---

## 🚀 部署步骤

### 方案 1: 使用 OpenAI

1. **获取 API Key**
   - 访问 https://platform.openai.com/api-keys
   - 创建新的 API Key

2. **更新 docker-compose.yml**
   ```yaml
   - OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxx
   - OPENAI_BASE_URL=https://api.openai.com/v1
   - OPENAI_MODEL=gpt-4o-mini
   ```

3. **推荐模型**
   - `gpt-4o-mini` - 性价比最高，速度快
   - `gpt-4o` - 最强能力，适合复杂任务
   - `gpt-4-turbo` - 平衡性能和成本

### 方案 2: 使用 DeepSeek (更便宜！)

1. **获取 API Key**
   - 访问 https://platform.deepseek.com/
   - 创建账号并获取 API Key

2. **更新 docker-compose.yml**
   ```yaml
   - OPENAI_API_KEY=sk-xxxxxxxxxxxxx
   - OPENAI_BASE_URL=https://api.deepseek.com
   - OPENAI_MODEL=deepseek-chat
   ```

3. **DeepSeek 优势**
   - 💰 价格便宜（约为 OpenAI 的 1/10）
   - 🌍 对海外服务器友好
   - 🚀 响应速度快
   - ✅ 完全兼容 OpenAI API

### 方案 3: 使用 Gemini (Google)

1. **获取 API Key**
   - 访问 https://aistudio.google.com/app/apikey
   - 创建 API Key

2. **使用 OpenRouter 作为代理**
   ```yaml
   - OPENAI_API_KEY=sk-or-v1-xxxxxxxxxxxxx
   - OPENAI_BASE_URL=https://openrouter.ai/api/v1
   - OPENAI_MODEL=google/gemini-pro-1.5
   ```

### 方案 4: 使用自定义 OpenAI 兼容服务

任何兼容 OpenAI API 格式的服务都可以使用：
```yaml
- OPENAI_API_KEY=your_api_key
- OPENAI_BASE_URL=https://your-custom-endpoint.com/v1
- OPENAI_MODEL=your-model-name
```

---

## 🔨 构建和部署

### 1. 本地测试
```bash
cd memes-master

# 编译项目
mvn clean package

# 运行测试
mvn test
```

### 2. Docker 部署

```bash
# 停止现有服务
docker compose down

# 重新构建镜像（如果本地构建）
docker compose build

# 或者推送代码到 GitHub，让 CI/CD 自动构建
git add .
git commit -m "Migrate from DashScope to OpenAI API"
git push

# 拉取最新镜像
docker compose pull

# 启动服务
docker compose up -d

# 查看日志确认启动成功
docker compose logs -f memes-app | grep -i "openai\|review"
```

### 3. 验证部署

查看日志，应该看到：
```
Spring AI OpenAI ChatModel initialized with model: gpt-4o-mini
Starting AI reviewer...
Starting sharp reviewer...
```

---

## 💡 配置示例

### 完整的 docker-compose.yml 配置

#### 方案 1: OpenAI (推荐用于生产环境)

```yaml
services:
  memes-app:
    image: ghcr.io/szemeng76/memes:latest
    container_name: memes-backend
    restart: unless-stopped
    ports:
      - "8081:8080"
    environment:
      # 数据库配置
      - jdbcUrl=jdbc:mysql://mysql:3306/memes?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true
      - jdbcUser=memes
      - jdbcPassword=your_password

      # Redis配置
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379

      # Spring AI OpenAI配置
      - OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxx
      - OPENAI_BASE_URL=https://api.openai.com
      - OPENAI_MODEL=gpt-4o-mini

      # 应用配置
      - token=your_app_token
      - SERVER_PORT=8080
      - SPRING_PROFILES_ACTIVE=prod
      - storage=local
      - urlPrefix=https://your-domain.com/

    volumes:
      - ./uploads:/memes
      - ./logs:/logs
    depends_on:
      - mysql
      - redis
    networks:
      - memes-network

  mysql:
    image: mysql:8.0
    container_name: memes-mysql
    restart: unless-stopped
    environment:
      - MYSQL_ROOT_PASSWORD=your_root_password
      - MYSQL_DATABASE=memes
      - MYSQL_USER=memes
      - MYSQL_PASSWORD=your_password
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "3308:3306"
    networks:
      - memes-network

  redis:
    image: redis:7-alpine
    container_name: memes-redis
    restart: unless-stopped
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    ports:
      - "6380:6379"
    networks:
      - memes-network

volumes:
  mysql_data:
  redis_data:

networks:
  memes-network:
```

#### 方案 2: DeepSeek (推荐用于成本优化)

只需修改 AI 相关配置：

```yaml
environment:
  # Spring AI DeepSeek配置
  - OPENAI_API_KEY=sk-xxxxxxxxxxxxx
  - OPENAI_BASE_URL=https://api.deepseek.com
  - OPENAI_MODEL=deepseek-chat
```

**DeepSeek 优势**：
- 💰 价格约为 OpenAI 的 1/10
- 🚀 对海外服务器友好，响应快
- ✅ 完全兼容 OpenAI API

#### 方案 3: 自建 OpenAI 兼容服务

```yaml
environment:
  # 自建 API 配置
  - OPENAI_API_KEY=your_api_key
  - OPENAI_BASE_URL=https://api.yourdomain.com
  - OPENAI_MODEL=gpt-4o-mini
```

**注意**：
- `OPENAI_BASE_URL` 不要包含 `/v1` 路径
- Spring AI 会自动添加 `/v1/chat/completions`
- 确保你的 API 支持 OpenAI 兼容格式

### 使用环境变量文件 (.env) - 推荐方式

创建 `.env` 文件（更安全，不提交到 Git）：

```env
# 数据库配置
MYSQL_PASSWORD=your_password
MYSQL_ROOT_PASSWORD=your_root_password

# OpenAI API配置
OPENAI_API_KEY=sk-xxxxxxxxxxxxx
OPENAI_BASE_URL=https://api.openai.com
OPENAI_MODEL=gpt-4o-mini

# 应用配置
APP_TOKEN=your_app_token
URL_PREFIX=https://your-domain.com/
```

更新 docker-compose.yml：

```yaml
services:
  memes-app:
    environment:
      # 数据库配置
      - jdbcUrl=jdbc:mysql://mysql:3306/memes?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true
      - jdbcUser=memes
      - jdbcPassword=${MYSQL_PASSWORD}

      # OpenAI配置 (从.env文件读取)
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - OPENAI_BASE_URL=${OPENAI_BASE_URL}
      - OPENAI_MODEL=${OPENAI_MODEL}

      # 应用配置
      - token=${APP_TOKEN}
      - urlPrefix=${URL_PREFIX}
      - SPRING_PROFILES_ACTIVE=prod
```

**重要**：将 `.env` 添加到 `.gitignore`！

```bash
echo ".env" >> .gitignore
```

---

## 🔍 故障排查

### 问题 1: "Unauthorized" 错误
**原因**: API Key 无效或过期

**解决**:
1. 检查 API Key 是否正确
   ```bash
   docker compose logs memes-app | grep "OPENAI_API_KEY"
   ```
2. 确认 API Key 有足够的配额
3. 验证 Base URL 是否正确

### 问题 2: "Invalid URL (POST /v1/v1/chat/completions)" 错误
**原因**: Base URL 配置包含了 `/v1`，导致路径重复

**解决**:
1. ❌ 错误配置：`OPENAI_BASE_URL=https://api.openai.com/v1`
2. ✅ 正确配置：`OPENAI_BASE_URL=https://api.openai.com`
3. Spring AI 会自动添加 `/v1/chat/completions`

### 问题 3: "Model not found" 错误
**原因**: 模型名称不正确

**解决**:
1. 检查模型名称拼写
   ```bash
   # 查看当前配置的模型
   docker compose exec memes-app env | grep OPENAI_MODEL
   ```
2. 确认该模型在你的 API 账号下可用
3. 常用模型名称：
   - OpenAI: `gpt-4o`, `gpt-4o-mini`, `gpt-4-turbo`
   - DeepSeek: `deepseek-chat`, `deepseek-reasoner`

### 问题 4: "Download the media resource timed out" 错误
**原因**: AI 服务无法访问图片 URL

**解决**:
1. 确保图片 URL 可以从公网访问
   ```bash
   curl -I https://your-domain.com/path/to/image.jpg
   ```
2. 检查防火墙设置，允许 AI 服务访问
3. 使用 CDN 加速图片访问
4. 确保 `urlPrefix` 配置正确

### 问题 5: Vision API 不工作
**原因**: 模型不支持 Vision 功能

**解决**:
1. 确保使用支持 Vision 的模型
   - ✅ `gpt-4o`, `gpt-4o-mini`
   - ✅ `deepseek-chat` (支持多模态)
   - ❌ `gpt-3.5-turbo` (不支持图片)
2. 检查图片格式是否支持（PNG, JPEG, WEBP, GIF）

### 问题 6: AI 服务没有启动
**原因**: Profile 配置或 Bean 初始化失败

**解决**:
1. 检查日志中是否有 "Starting AI reviewer"
   ```bash
   docker compose logs memes-app | grep -i "starting.*review"
   ```
2. 确认 Profile 设置为 `prod`
   ```bash
   docker compose exec memes-app env | grep SPRING_PROFILES_ACTIVE
   ```
3. 检查 API Key 是否正确配置

### 问题 7: 应用启动失败 "DuplicateKeyException"
**原因**: application.yaml 中有重复的键

**解决**:
1. 检查 YAML 文件，确保没有重复的顶级键
2. 所有 `spring:` 配置应该在同一个块下
3. 使用 YAML linter 验证文件格式

---

## 📊 成本对比

| 服务 | 输入价格 (每 1M tokens) | 输出价格 (每 1M tokens) | 特点 |
|------|------------------------|------------------------|------|
| **OpenAI GPT-4o-mini** | $0.15 | $0.60 | 性价比高，推荐 |
| **OpenAI GPT-4o** | $2.50 | $10.00 | 能力最强 |
| **DeepSeek** | $0.14 | $0.28 | 最便宜，速度快 |
| **Gemini Pro** | $0.125 | $0.375 | Google 服务 |

💡 **建议**: 开发/测试用 DeepSeek，生产环境用 GPT-4o-mini

---

## 🌟 新特性

### 1. 多提供商支持
可以轻松切换不同的 AI 服务提供商，无需修改代码

### 2. 更好的海外访问
OpenAI 和 DeepSeek 对海外服务器更友好，没有 DashScope 的超时问题

### 3. 更灵活的配置
通过环境变量控制模型选择，方便 A/B 测试

### 4. 成本优化
可以选择更便宜的服务商，降低运营成本

---

## 📈 监控和指标

本应用通过 Micrometer 向 InfluxDB 发送 AI 使用指标：

### 可用指标

| 指标名称 | 类型 | 标签 | 说明 |
|---------|------|------|------|
| `total_token` | Counter | model | 总 Token 消耗 |
| `input_token` | Counter | model | 输入 Token 消耗 |
| `output_token` | Counter | model | 输出 Token 消耗 |
| `llm_api_error` | Counter | model | API 调用错误次数 |
| `llm_inappropriate_content` | Counter | model | 违规内容检测次数 |
| `llm_review_count` | Counter | outcome | 审核结果分布 (APPROVED/FLAGGED/REJECTED) |

### 启用 InfluxDB 监控

在 docker-compose.yml 中配置：

```yaml
environment:
  # 启用InfluxDB监控
  - MANAGEMENT_METRICS_EXPORT_INFLUX_ENABLED=true

  # InfluxDB监控配置
  - influxUrl=http://influxdb:8086
  - influxBucket=memes
  - influxOrg=memes
  - influxToken=szemeng90
```

### 查看指标

1. **访问 InfluxDB UI**: http://your-server:8086
2. **登录信息**:
   - Username: admin
   - Password: 配置的密码
3. **查询示例**:
   ```flux
   from(bucket: "memes")
     |> range(start: -1h)
     |> filter(fn: (r) => r._measurement == "total_token")
     |> aggregateWindow(every: 5m, fn: sum)
   ```

### 成本估算

通过监控 Token 使用量来估算成本：

```bash
# 查看今日总 Token 消耗
docker compose logs memes-app | grep "total_tokens=" | awk '{sum+=$NF} END {print "Total tokens:", sum}'

# 估算成本（以 GPT-4o-mini 为例）
# 输入: $0.15/1M tokens
# 输出: $0.60/1M tokens
```

---

## ⚙️ 高级配置

### 1. 调整 AI 审核参数

修改 `AiReviewer.java` 中的参数：

```java
var chatOptions = OpenAiChatOptions.builder()
    .model(model)
    .temperature(0.0)      // 降低随机性，提高一致性
    .maxTokens(1000)       // 控制输出长度
    .build();
```

**参数说明**:
- `temperature`: 0.0-2.0，越低越确定，越高越随机
- `maxTokens`: 限制输出长度，控制成本
- `topP`: 0.0-1.0，控制采样多样性（与 temperature 二选一）

### 2. 自定义审核提示词

修改 `src/main/resources/prompt.xml` 来调整审核标准：

```xml
<system>
你是一个专业的内容审核助手。请根据以下标准审核图片：
1. 不包含暴力、血腥内容
2. 不包含政治敏感内容
3. 不包含色情或性暗示内容
4. 符合社区规范

返回格式：
{
  "outcome": "APPROVED/FLAGGED/REJECTED",
  "mediaDescription": "图片描述",
  "failureReason": "拒绝原因（如有）"
}
</system>
```

### 3. 调整审核并发

修改 `AiReviewer.java` 中的线程池配置：

```java
// 单线程执行（当前配置）- 成本可控
private final ExecutorService reviewExecutor =
    Executors.newSingleThreadExecutor();

// 多线程执行 - 处理速度更快但成本更高
private final ExecutorService reviewExecutor =
    Executors.newFixedThreadPool(3);
```

### 4. 配置重试策略

在 application.yaml 中添加：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
          temperature: 0.0
      # 重试配置
      retry:
        max-attempts: 3
        backoff:
          initial-interval: 2000
          multiplier: 2
          max-interval: 10000
```

### 5. 使用代理服务器

如果需要通过代理访问 OpenAI：

```yaml
services:
  memes-app:
    environment:
      - OPENAI_API_KEY=sk-xxxxxxxxxxxxx
      - OPENAI_BASE_URL=https://api.openai.com
      - OPENAI_MODEL=gpt-4o-mini

      # HTTP 代理配置
      - HTTP_PROXY=http://proxy.example.com:8080
      - HTTPS_PROXY=http://proxy.example.com:8080
      - NO_PROXY=localhost,127.0.0.1
```

---

## 🚄 快速参考

### 常用命令

```bash
# 查看应用日志
docker compose logs -f memes-app

# 只看 AI 相关日志
docker compose logs -f memes-app | grep -i "openai\|review\|llm"

# 查看错误日志
docker compose logs memes-app | grep -i "error\|exception"

# 重启应用
docker compose restart memes-app

# 查看容器状态
docker compose ps

# 进入容器
docker compose exec memes-app sh

# 查看环境变量
docker compose exec memes-app env | grep OPENAI

# 清理并重新部署
docker compose down -v && docker compose up -d

# 查看资源使用
docker stats memes-app
```

### 环境变量快速参考

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `OPENAI_API_KEY` | ✅ | - | OpenAI API 密钥 |
| `OPENAI_BASE_URL` | ✅ | `https://api.openai.com` | API 基础 URL（不含 /v1） |
| `OPENAI_MODEL` | ✅ | `gpt-4o-mini` | 使用的模型名称 |
| `SPRING_PROFILES_ACTIVE` | ✅ | `prod` | Spring Profile（必须为 prod 才启用 AI） |
| `jdbcUrl` | ✅ | - | 数据库连接 URL |
| `jdbcUser` | ✅ | - | 数据库用户名 |
| `jdbcPassword` | ✅ | - | 数据库密码 |
| `token` | ✅ | - | 应用访问 Token |
| `urlPrefix` | ✅ | - | 图片访问 URL 前缀 |
| `storage` | ⭕ | `local` | 存储方式 |
| `MANAGEMENT_METRICS_EXPORT_INFLUX_ENABLED` | ⭕ | `false` | 是否启用 InfluxDB 监控 |

### API 提供商配置对比

| 提供商 | OPENAI_BASE_URL | OPENAI_MODEL | 特点 |
|--------|-----------------|--------------|------|
| **OpenAI** | `https://api.openai.com` | `gpt-4o-mini` | 官方服务，稳定可靠 |
| **DeepSeek** | `https://api.deepseek.com` | `deepseek-chat` | 便宜快速，中国友好 |
| **自建服务** | `https://api.smone.me` | `gpt-4o-mini` | 自定义端点 |
| **Azure OpenAI** | `https://your-resource.openai.azure.com` | `gpt-4o` | 企业级，需要特殊配置 |

### 模型选择建议

| 场景 | 推荐模型 | 原因 |
|------|---------|------|
| **开发测试** | `deepseek-chat` | 成本低，速度快 |
| **生产环境（图片审核）** | `gpt-4o-mini` | 性价比高，Vision 能力强 |
| **生产环境（文本生成）** | `deepseek-chat` | 中文效果好，成本低 |
| **复杂审核任务** | `gpt-4o` | 理解能力最强，准确度高 |
| **预算有限** | `deepseek-chat` | 最便宜的选择 |

---

## 📚 相关文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI OpenAI 配置](https://docs.spring.io/spring-ai/reference/api/clients/openai-chat.html)
- [OpenAI API 文档](https://platform.openai.com/docs)
- [DeepSeek API 文档](https://platform.deepseek.com/docs)
- [OpenRouter 文档](https://openrouter.ai/docs)

---

## 🤝 支持

如遇到问题，请按以下步骤排查：

1. **检查配置**
   ```bash
   docker compose exec memes-app env | grep OPENAI
   docker compose exec memes-app env | grep SPRING_PROFILES_ACTIVE
   ```

2. **查看日志**
   ```bash
   docker compose logs -f memes-app | grep -i "error\|exception\|openai"
   ```

3. **测试 API 连接**
   ```bash
   curl -I https://your-base-url.com
   ```

4. **验证 API Key**
   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer $OPENAI_API_KEY"
   ```

5. **检查网络**
   - API 服务器能否访问？
   - 图片 URL 能否从外网访问？
   - 防火墙是否允许？

如果以上步骤都无法解决问题，请提供：
- 完整的错误日志
- 环境变量配置（隐藏敏感信息）
- 使用的模型和 Base URL

---

## 📝 更新日志

### v2.0.0 (2025-11-03)
- ✅ 完全移除 DashScope SDK
- ✅ 迁移到 Spring AI 1.0.1 OpenAI
- ✅ 支持 OpenAI、DeepSeek、Gemini 等多种 API
- ✅ 支持自定义 OpenAI 兼容端点
- ✅ 改进错误处理和重试机制
- ✅ 添加详细的使用指标监控
- ✅ 完善文档和故障排查指南

### v1.0.0 (之前)
- 使用 Alibaba DashScope SDK
- 仅支持通义千问模型

---

**迁移完成日期**: 2025-11-03
**维护者**: Claude Code AI Assistant
**项目状态**: ✅ 生产就绪
