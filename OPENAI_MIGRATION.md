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
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
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

### OpenAI (推荐用于生产环境)
```yaml
environment:
  - OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxx
  - OPENAI_BASE_URL=https://api.openai.com/v1
  - OPENAI_MODEL=gpt-4o-mini
```

### DeepSeek (推荐用于成本优化)
```yaml
environment:
  - OPENAI_API_KEY=sk-xxxxxxxxxxxxx
  - OPENAI_BASE_URL=https://api.deepseek.com
  - OPENAI_MODEL=deepseek-chat
```

### 使用环境变量文件 (.env)
创建 `.env` 文件：
```env
OPENAI_API_KEY=sk-xxxxxxxxxxxxx
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o-mini
OPENAI_VISION_MODEL=gpt-4o-mini
```

更新 docker-compose.yml：
```yaml
services:
  memes-app:
    env_file:
      - .env
```

---

## 🔍 故障排查

### 问题 1: "Unauthorized" 错误
**原因**: API Key 无效或过期

**解决**:
1. 检查 API Key 是否正确
2. 确认 API Key 有足够的配额
3. 验证 Base URL 是否正确

### 问题 2: "Model not found" 错误
**原因**: 模型名称不正确

**解决**:
1. 检查模型名称拼写
2. 确认该模型在你的 API 账号下可用
3. 参考 API 文档确认正确的模型名称

### 问题 3: 超时错误
**原因**: 网络连接问题或图片 URL 无法访问

**解决**:
1. 确保服务器可以访问 OpenAI API
2. 检查图片 URL 是否可以从外网访问
3. 考虑增加超时时间或使用 CDN

### 问题 4: Vision API 不工作
**原因**: 模型不支持 Vision 功能

**解决**:
1. 确保使用支持 Vision 的模型（如 `gpt-4o`, `gpt-4o-mini`）
2. 旧模型如 `gpt-3.5-turbo` 不支持图片分析
3. DeepSeek 的 `deepseek-chat` 支持多模态

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

## 📚 相关文档

- [OpenAI API 文档](https://platform.openai.com/docs)
- [DeepSeek API 文档](https://platform.deepseek.com/docs)
- [simple-openai GitHub](https://github.com/sashirestela/simple-openai)
- [OpenRouter 文档](https://openrouter.ai/docs)

---

## 🤝 支持

如遇到问题，请检查：
1. API Key 是否有效
2. Base URL 配置是否正确
3. 模型名称是否支持
4. 网络连接是否正常
5. 查看应用日志获取详细错误信息

---

**迁移完成日期**: 2025-11-03
**维护者**: Claude Code AI Assistant
