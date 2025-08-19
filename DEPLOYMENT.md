# 部署指南

## 前置准备

### 1. VPS要求
- Ubuntu 20.04+ / CentOS 7+
- 2GB+ RAM
- 20GB+ 存储空间
- Docker & Docker Compose

### 2. 安装Docker和Docker Compose
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

## 部署步骤

### 1. 克隆项目
```bash
git clone https://github.com/YOUR_USERNAME/memes-master.git
cd memes-master
```

### 2. 配置环境变量
```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
nano .env
```

**重要配置项：**
- `MYSQL_ROOT_PASSWORD`: MySQL root密码
- `MYSQL_PASSWORD`: 应用数据库密码
- `DASHSCOPE_API_KEY`: 阿里云通义千问API密钥
- `DOMAIN_NAME`: 你的域名

### 3. 修改docker-compose.yml
```bash
nano docker-compose.yml
```

**修改镜像地址：**
```yaml
services:
  memes-app:
    image: ghcr.io/YOUR_USERNAME/memes-master:latest  # 替换YOUR_USERNAME
```

### 4. 创建必要目录
```bash
mkdir -p uploads logs ssl
chmod 755 uploads logs
```

### 5. 启动服务
```bash
# 拉取最新镜像
docker-compose pull

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f memes-app
```

### 6. 初始化数据库
数据库会自动使用 `sql.sql` 文件初始化，如果需要手动执行：
```bash
docker-compose exec mysql mysql -u root -p memes < sql.sql
```

## GitHub Actions设置

### 1. 启用GitHub Packages
1. 进入GitHub仓库设置
2. 确保Actions有写入packages权限

### 2. 自动部署
当推送到main/master分支时，GitHub Actions会自动：
- 构建Docker镜像
- 推送到GitHub Container Registry

### 3. 手动拉取更新
```bash
# 拉取最新镜像
docker-compose pull memes-app

# 重启服务
docker-compose up -d memes-app
```

## 域名和SSL配置

### 1. 域名设置
将域名A记录指向VPS IP地址

### 2. SSL证书 (使用Let's Encrypt)
```bash
# 安装certbot
sudo apt install certbot

# 获取SSL证书
sudo certbot certonly --standalone -d your-domain.com

# 复制证书
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem ./ssl/
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem ./ssl/
sudo chown $USER:$USER ./ssl/*
```

### 3. 启用HTTPS
编辑 `nginx.conf`，取消HTTPS配置的注释，然后重启nginx：
```bash
docker-compose restart nginx
```

## 服务管理

### 常用命令
```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs [service_name]

# 重启服务
docker-compose restart [service_name]

# 停止所有服务
docker-compose down

# 更新并重启
docker-compose pull && docker-compose up -d
```

### 备份数据
```bash
# 备份数据库
docker-compose exec mysql mysqldump -u root -p memes > backup.sql

# 备份上传文件
tar -czf uploads_backup.tar.gz uploads/
```

## 监控和维护

### 1. 健康检查
访问 `http://your-domain.com/health` 检查应用状态

### 2. 日志监控
```bash
# 实时查看应用日志
docker-compose logs -f memes-app

# 查看nginx访问日志
docker-compose logs nginx
```

### 3. 资源监控
```bash
# 查看容器资源使用
docker stats

# 查看磁盘使用
df -h
```

## 故障排除

### 常见问题

1. **应用启动失败**
   ```bash
   docker-compose logs memes-app
   ```

2. **数据库连接失败**
   - 检查密码配置
   - 确保MySQL容器已启动

3. **文件上传失败**
   - 检查uploads目录权限
   - 确保磁盘空间充足

4. **SSL证书问题**
   - 检查证书文件路径
   - 确保证书未过期

### 性能优化

1. **数据库优化**
   - 定期清理日志
   - 优化MySQL配置

2. **文件存储优化**
   - 使用对象存储服务（阿里云OSS/七牛云）
   - 定期清理临时文件

3. **缓存优化**
   - 启用Redis缓存
   - 配置nginx缓存

## 安全建议

1. **防火墙设置**
   ```bash
   sudo ufw allow 80
   sudo ufw allow 443
   sudo ufw allow 22
   sudo ufw enable
   ```

2. **定期更新**
   - 更新系统包
   - 更新Docker镜像
   - 更新SSL证书

3. **备份策略**
   - 每日数据库备份
   - 定期文件备份
   - 异地备份存储