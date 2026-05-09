# CookieMusic

CookieMusic 是一个面向本科毕设的 AI 音乐创作与分享平台，包含用户前台、管理后台、Spring Boot 后端、MySQL/Redis 数据层，以及本地 MusicGen 纯音乐生成服务。

## 模块说明

- `cookiemusic-front/cookiemusic-front-web`: 用户前台，Vue 3 + Vite，默认端口 `3006`
- `cookiemusic-front/cookiemusic-front-admin`: 管理后台，Vue 3 + Vite，默认端口 `3007`
- `cookiemusic-java/cookiemusic-web`: 用户端后端服务，Spring Boot，默认端口 `8090`
- `cookiemusic-java/cookiemusic-admin`: 管理端后端服务，Spring Boot，默认端口 `8091`
- `cookiemusic-java/cookiemusic-common`: 公共实体、Mapper、Service、Redis、支付、AI 接口封装
- `cookiemusic-musicgen`: 本地 MusicGen Flask 服务，默认端口 `8092`
- `cookiemusic.sql`: MySQL 初始化脚本

## 核心功能

- 用户注册、登录、验证码、个人资料、密码修改
- AI 音乐创作，支持歌曲和纯音乐两种类型
- 歌曲生成预留天谱乐接口，纯音乐使用本地 MusicGen Small
- 音乐播放、详情、点赞、分享、封面上传、标题修改
- 首页混合推荐，包含内容推荐、User-CF、Item-CF、热度和时间衰减
- 积分系统，支持创作扣减、充值到账、失败回滚、积分流水
- 付款码支付，后台生成一次性 8 位付款码，前台输入后积分到账
- 管理后台支持用户、商品、订单、音乐、字典、付款码管理

## 本地启动

1. 导入数据库。

```bash
mysql -uroot -p easymusic < cookiemusic.sql
```

2. 启动 Redis，默认端口 `6379`。

3. 启动 MusicGen 服务。

```bash
cd cookiemusic-musicgen
pip install -r requirements.txt
python server.py
```

4. 启动 Java 用户端和管理端。

```bash
cd cookiemusic-java
mvn spring-boot:run -pl cookiemusic-web
mvn spring-boot:run -pl cookiemusic-admin
```

5. 启动前台和后台。

```bash
cd cookiemusic-front/cookiemusic-front-web
npm install
npm run dev

cd ../cookiemusic-front-admin
npm install
npm run dev
```

## 默认地址

- 用户前台: `http://localhost:3006`
- 管理后台: `http://localhost:3007`
- 用户端接口: `http://localhost:8090/api`
- 管理端接口: `http://localhost:8091/api`
- MusicGen 服务: `http://localhost:8092/api/health`

## 说明

当前毕设演示主支付流程为付款码支付，不依赖在线商户资质。代码中保留了第三方支付接口封装、回调和轮询能力，作为后续扩展设计。
