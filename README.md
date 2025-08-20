# Memes Backend API

专为北邮人设计的贴图秀投稿系统后端API服务。

## 技术栈

- **框架**: Spring Boot 3.4.3
- **语言**: Java 21 (启用预览特性)
- **数据库**: MySQL (生产) / H2 (测试)
- **ORM**: MyBatis Plus
- **存储服务**: 
  - 阿里云OSS
  - 七牛云存储
  - 本地存储 (开发测试)
- **AI服务**: 阿里云通义千问 (内容审核)
- **监控**: Micrometer + InfluxDB
- **构建工具**: Maven
- **代码规范**: Spotless + Eclipse Formatter

## 主要功能

### 核心API
- **投稿管理** (`/api/submission`)
  - 分页列表查询 (支持随机排序)
  - 内容更新和删除 (需要管理员权限)
  - 点赞/反馈统计
  - 内容置顶功能
- **媒体内容** (`/api/media`)
  - 文件上传 (图片/视频)
  - 多种存储后端支持
- **管理功能** (`/api/admin`)
  - 访问统计
  - 系统配置管理
- **配置管理** (`/api/config`)
  - 动态配置更新

### 智能审核
- **AI内容审核**: 集成阿里云通义千问，自动识别不当内容
- **定时任务**: 自动化内容审核和统计分析
- **Sharp Review**: 基于规则的快速审核

### 存储支持
- **多云存储**: 支持阿里云OSS、七牛云存储
- **本地存储**: 开发测试环境支持
- **自动切换**: 根据配置自动选择存储后端

## 快速开始

### 使用 Docker (推荐)

```bash
# 拉取预构建镜像
docker pull ghcr.io/szemeng76/memes:latest

# 运行容器
docker run -d \
  --name memes-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e jdbcUrl=jdbc:mysql://your-mysql-host:3306/memes?useSSL=false&serverTimezone=UTC&characterEncoding=utf8 \
  -e jdbcUser=your-username \
  -e jdbcPassword=your-password \
  -e dashscopeApiKey=your-ai-api-key \
  -e token=your-admin-token \
  -e storage=local \
  -e urlPrefix=http://localhost:8080 \
  ghcr.io/szemeng76/memes:latest
```

### 使用 Docker Compose

```yaml
version: '3.8'
services:
  memes-api:
    image: ghcr.io/szemeng76/memes:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - jdbcUrl=jdbc:mysql://mysql:3306/memes?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
      - jdbcUser=memes
      - jdbcPassword=your-password
      - dashscopeApiKey=your-ai-api-key
      - token=your-admin-token
      - storage=local
      - urlPrefix=http://localhost:8080
    depends_on:
      - mysql
    volumes:
      - ./uploads:/memes
    
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root-password
      - MYSQL_DATABASE=memes
      - MYSQL_USER=memes
      - MYSQL_PASSWORD=your-password
      - MYSQL_CHARACTER_SET_SERVER=utf8mb4
      - MYSQL_COLLATION_SERVER=utf8mb4_unicode_ci
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql.sql:/docker-entrypoint-initdb.d/init.sql

volumes:
  mysql_data:
```

### 本地开发

```bash
# 克隆项目
git clone <your-repo>
cd memes-master

# 安装依赖并编译
mvn clean compile

# 运行测试
mvn test

# 启动开发服务器
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 运行环境 | `dev` |
| `jdbcUrl` | 数据库连接URL | - |
| `jdbcUser` | 数据库用户名 | - |
| `jdbcPassword` | 数据库密码 | - |
| `dashscopeApiKey` | 通义千问API密钥 | - |
| `token` | 管理员认证Token | - |
| `storage` | 存储类型 | `local` |
| `urlPrefix` | 文件访问URL前缀 | `localhost:8080` |

### 配置文件

- `application.yaml` - 基础配置
- `application-dev.yaml` - 开发环境配置  
- `application-prod.yaml` - 生产环境配置

### 存储配置

支持多种存储后端，通过配置切换：

```yaml
storage:
  type: qiniu  # aliyun, qiniu, local
  qiniu:
    accessKey: your-access-key
    secretKey: your-secret-key
    bucket: your-bucket
  aliyun:
    endpoint: your-endpoint
    accessKeyId: your-access-key-id
    accessKeySecret: your-access-key-secret
    bucketName: your-bucket
```

## API文档

### 投稿相关

```
GET    /api/submission              # 获取投稿列表
PUT    /api/submission/{id}         # 更新投稿 (需要auth)
DELETE /api/submission/{id}         # 删除投稿 (需要auth)
POST   /api/submission/{id}/feedback/{isLike}  # 点赞/反馈
POST   /api/submission/{id}/pin     # 置顶投稿 (需要auth)
```

### 媒体文件

```
POST   /api/media/upload           # 上传文件
GET    /api/media/download/{filename}  # 下载文件
```

### 管理接口

```
GET    /api/admin/statistic        # 获取统计数据 (需要auth)
GET    /api/config                 # 获取配置信息
```

## 部署架构

### 推荐部署方案

```
[Nginx/CDN] -> [Application] -> [MySQL]
                    |
                    v
            [File Storage (OSS/七牛云)]
                    |
                    v
            [AI Service (通义千问)]
```

### 监控和运维

- **健康检查**: `/actuator/health`
- **指标监控**: `/actuator/metrics`
- **InfluxDB集成**: 自动发送运行时指标
- **日志**: 使用Logback，支持不同环境的日志级别

## 数据库初始化

执行 `sql.sql` 文件创建所需的数据表：

```bash
mysql -u username -p database_name < sql.sql
```

主要表结构：
- `submission` - 投稿内容
- `media_content` - 媒体文件信息
- `config` - 系统配置
- `request_log` - 请求日志
- `pinned_submission` - 置顶内容

## 开发指南

### 项目结构

```
src/main/java/com/memes/
├── annotation/          # 自定义注解
├── aspect/             # AOP切面
├── config/             # 配置类
├── controller/         # REST控制器
├── exception/          # 异常处理
├── mapper/             # MyBatis映射器
├── model/              # 数据模型
├── schedule/           # 定时任务
├── service/            # 业务服务
└── util/               # 工具类
```

### 主要组件

- **认证系统**: 基于注解的简单认证机制
- **文件上传**: 支持多种存储后端的统一接口
- **内容审核**: AI+规则双重审核机制
- **缓存机制**: 配置热重载和请求缓存
- **监控集成**: 自动指标收集和上报

### 代码规范

项目使用 Spotless 进行代码格式化：

```bash
# 检查代码格式
mvn spotless:check

# 自动格式化代码  
mvn spotless:apply
```

## 性能优化

- **懒加载**: Spring应用懒初始化
- **连接池**: HikariCP数据库连接池
- **缓存策略**: 配置和媒体内容缓存
- **异步处理**: AI审核异步执行
- **资源优化**: 静态资源CDN加速

## 故障排查

### 常见问题

1. **数据库连接失败**
   - 检查数据库服务是否启动
   - 确认连接参数正确
   - 查看防火墙设置

2. **文件上传失败**
   - 检查存储服务配置
   - 确认访问密钥有效
   - 查看文件大小限制

3. **AI审核不工作**
   - 确认通义千问API密钥有效
   - 检查网络连接
   - 查看API调用限额

### 日志查看

```bash
# 查看应用日志
docker logs memes-api

# 实时跟踪日志
docker logs -f memes-api
```

## 贡献指南

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目仅供学习交流使用。

## 联系方式

如有问题或建议，请通过 Issues 或 Discussions 联系。