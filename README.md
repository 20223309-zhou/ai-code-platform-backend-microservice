# iCode — AI 驱动的应用生成平台（Spring Cloud 微服务版）

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.0.0-6DB33F)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring_Cloud_Alibaba-2025.0.0.0-orange)](https://spring.io/projects/spring-cloud-alibaba)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.15-000000?logo=langchain)](https://docs.langchain4j.dev)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

> 基于 LangChain4j 的 AI 代码生成平台，支持自然语言生成 HTML / 多文件 / Vue 工程代码，具备 RAG 知识库检索、
> Skill 技能按需注入、多模态文件上传、流式输出与实时预览能力。
> 微服务侧采用 **Spring Cloud Alibaba + OpenFeign + Gateway** 做服务治理与统一入口，**RabbitMQ** 承担日志与截图的异步解耦。

---

## 目录

- [分支说明](#分支说明)
- [架构](#架构)
- [模块一览](#模块一览)
- [端口一览](#端口一览)
- [通信方式](#通信方式)
- [网关路由表](#网关路由表)
- [消息队列设计](#消息队列设计)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [关键配置与必填项](#关键配置与必填项)
- [核心功能](#核心功能)
- [RAG 知识库](#rag-知识库)
- [Skill 技能集成](#skill-技能集成)
- [常见问题](#常见问题)
- [License](#license)

---

## 分支说明

| 分支 | 说明 |
|---|---|
| `master` | 初始版本 |
| `v1` | 微服务拆分，服务治理使用 **Apache Dubbo + Nacos** |
| `v2` | **当前分支**：Dubbo 替换为 **Spring Cloud OpenFeign + Gateway**，统一 HTTP 入口 |

> 同系列的另外两个仓库：后端单体版（`ai-code-platform-backend`）与前端（`ai-code-platform-frontend`）。

---

## 架构

### 请求入口：前端 → 网关 → 服务

```text
[前端 Vue 3 :5173]
        │  /api/**
        ▼
[API 网关 ai-code-gateway :8100] ──── 服务发现 ────▶ [Nacos :8848]
        │
        ├── /api/app/**  /api/chatHistory/**  /api/statistics/**  ──▶ app-service  :8125
        ├── /api/user/**                                          ──▶ user-service :8124
        └── /api/sysOperationLog/**                               ──▶ log-service  :8127
```

### 服务间同步调用（OpenFeign）

```text
app-service  ──▶ user-service        GET/POST /api/inner/user/**
app-service  ──▶ log-service         GET      /api/inner/sysLog/**
screenshot   ──▶ app-service         PUT      /api/inner/app/update
```

### 异步任务（RabbitMQ）

```text
app-service ──[log.topic / log.record]────────────▶ log.queue1        ──▶ log-service
app-service ──[screenshot.topic / screenshot.generate]──▶ screenshot.queue1 ──▶ screenshot-service
```

### 共享基础设施

| 组件 | 用途 |
|---|---|
| MySQL | 所有服务连同一个库（微服务版不分主从） |
| Redis | Session 共享（namespace `ai-code:session`）+ 统计缓存 |
| Nacos | 服务注册与发现 |
| Qdrant | RAG 向量库（仅 app-service 使用，可回退 InMemory） |
| 腾讯云 COS | 头像、聊天图片、应用封面、截图产物 |


### 模块一览

共 **9 个** Maven 模块。

| 模块 | 说明 | 类型 | 端口 |
|---|---|---|---|
| **ai-code-common** | 工具类、异常体系、常量、安全、COS、限流配置 | JAR | — |
| **ai-code-model** | Entity / DTO / VO / 枚举 | JAR | — |
| **ai-code-client** | 跨服务接口契约（**Feign 客户端**） | JAR | — |
| **ai-code-ai** | LLM 调度、Function Calling 工具、RAG、提示词 | JAR（**不单独启动**） | — |
| **ai-code-user-service** | 注册登录、邮箱验证、VIP、额度 | 可运行服务 | 8124 |
| **ai-code-app-service** | 代码生成编排、对话、模板、统计 | 可运行服务 | 8125 |
| **ai-code-screenshot-service** | 消费 MQ 截图任务，Selenium 渲染后传 COS | 可运行服务 | 8126 |
| **ai-code-log-service** | 消费 MQ 写日志 + 提供查询 API | 可运行服务 | 8127 |
| **ai-code-gateway-service** | Spring Cloud Gateway，统一 HTTP 入口 | 可运行服务 | 8100 |

> `ai-code-ai` 是纯 JAR，被 `app-service` 依赖，**没有启动类**，不要试图单独运行。

---

## 端口一览

| 服务 | 端口 | 上下文路径 |
|---|---|---|
| 前端 Vite Dev Server | 5173 | — |
| **网关** | **8100** | `/`（无 context-path） |
| app-service | 8125 | `/api` |
| user-service | 8124 | `/api` |
| screenshot-service | 8126 | `/api` |
| log-service | 8127 | `/api` |
| Nacos | 8848 | — |
| RabbitMQ | 5672（管理台 15672） | — |

---

## 通信方式

| 方式 | 场景 | 实现 |
|---|---|---|
| **HTTP（OpenFeign）** | 服务间同步调用 | `ai-code-client` 定义 `@FeignClient` 接口，`lb://服务名` 经 Nacos + LoadBalancer 解析 |
| **RabbitMQ** | 异步任务（日志、截图） | 两个独立 Topic Exchange，各自队列 |
| **Gateway** | 前端 → 后端唯一入口 | 按路径前缀路由到 `lb://服务名` |

### 一次请求怎么走（`/api` 前缀为什么出现两次）

```
浏览器  GET /api/user/get/login
  ↓ Vite 代理 → 网关 :8100
网关    匹配 Path=/api/user/***
  ↓ lb://ai-code-user，路径原样转发（没有 StripPrefix）
user-service 收到 /api/user/get/login
  ↓ context-path=/api 吃掉前缀
  ↓ Spring MVC 匹配 /user/get/login
```

结论：

- 四个服务都配了 `server.servlet.context-path: /api`；
- 网关**不剥前缀**，所以断言里必须带 `/api`；
- Feign 的 `@FeignClient(path = "/api/inner/xxx")` 里的 `/api` 也是给对端 context-path 用的。

三处前缀必须一致，改任意一处都要同步另外两处，否则 404。

### 网关路由表

| 路径前缀 | 目标服务 |
|---|---|
| `/api/app/**`、`/api/chatHistory/**`、`/api/static/**`、`/api/statistics/**`、`/api/health/**` | `lb://ai-code-app` |
| `/api/user/**` | `lb://ai-code-user` |
| `/api/sysOperationLog/**` | `lb://ai-code-log` |

> `/api/inner/**` 是**服务间内部接口**（Feign 直连 `lb://服务名`），**故意不在网关暴露**。
>
> ⚠️ 路径前缀与所在服务必须对应：`chatHistory` / `static` / `statistics` 的 Controller 都在
> `app-service`，配到 user 上会直接 404。

---

## 消息队列设计

两套独立的 Topic Exchange，互不干扰：

| 生产者 | Exchange | Routing Key | 队列 | 消费者 |
|---|---|---|---|---|
| app-service | `log.topic` | `log.record` | `log.queue1` | log-service × 2（竞争消费） |
| app-service | `screenshot.topic` | `screenshot.generate` | `screenshot.queue1` | screenshot-service × 2 |

消费者参数（两个服务一致）：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 1            # 一次只取一条，处理完再取下一条
        acknowledge-mode: auto # 正常返回即 ACK，抛异常交给重试
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2.0
          max-interval: 10000
```

- 队列 `durable=true`，消息持久化；
- 同一队列挂两个监听方法 = **竞争消费**（并发度 2），消息不会重复入库；
- 日志队列在消费端把 `operation` 截断到 80 字符，避免超长导致整条写库失败。

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|---|---|---|
| Java | 21 | 开发语言 |
| Spring Boot | 3.5.3 | 基础框架 |
| Spring Cloud | 2025.0.0 | 微服务基础设施 |
| Spring Cloud Alibaba | 2025.0.0.0 | Nacos 注册发现 |
| Spring Cloud Gateway | 4.3.0 | API 网关（WebFlux） |
| OpenFeign + LoadBalancer | 4.3.0 | 服务间 HTTP 调用 |
| Nacos | 2.x | 注册中心 |
| RabbitMQ | 4.1.5 | 异步消息 |
| MyBatis-Flex | 1.11.0 | ORM |
| MySQL | 8.x | 业务数据库 |
| Redis | 7.x | 分布式 Session + 缓存 |
| LangChain4j | 1.15.0 | AI 编排、工具调用、RAG、Skills |
| Qdrant | 1.17 | 向量库（可回退 InMemory） |
| Redisson | 3.50.0 | 分布式限流 |
| Selenium | 4.33.0 | 网页截图 |
| 腾讯云 COS | 5.6.227 | 对象存储 |

### 前端

Vue 3.5 / TypeScript 5.8 / Vite 7 / Ant Design Vue / Pinia

### AI 模型

| 用途 | 模型 |
|---|---|
| 代码生成（可选） | `deepseek-v4.1-flash`（默认）、`grok-4.7`、`gpt-6.1-sol` |
| 非流式生成 | `deepseek-v4-flash` |
| 意图路由分类 | `qwen-flash` |
| 文本嵌入 | BAAI `bge-small-zh-v1.5`（512 维，本地运行） |

> **Logo 不走大模型**：`GenerateLogoSvg` 工具从 Iconify 检索真实图标后拼装 SVG，
> 拿不到图标时降级为抽象几何图形。

---

## 项目结构

```text
ai-code-platform-microservice/
├── pom.xml                          # 父 POM（聚合 9 个模块，统一管理 Boot / Cloud 版本）
├── sql/                             # 建表与增量脚本
├── tmp/                             # 运行时生成的代码与截图（git ignored）
│
├── ai-code-common/                  # 公共能力
│   └── .../com/ai/codeplatform/
│       ├── annotation/              #   @AuthCheck / @LogRecord / @InnerApi
│       ├── common/                  #   BaseResponse、分页
│       ├── config/                  #   CORS、COS、Security
│       ├── constant/                #   常量
│       ├── exception/               #   BusinessException、GlobalExceptionHandler、InnerApiExceptionHandler
│       ├── manager/                 #   CosManager、CancelGenerationManager
│       ├── ratelimit/               #   限流注解 + RedissonConfig
│       └── utils/                   #   IpUtils 等
│
├── ai-code-model/                   # 数据模型
│   └── .../com/ai/codeplatform/ai/model/{entity,dto,vo,enums}
│
├── ai-code-client/                  # 跨服务契约（Feign）
│   └── .../innerservice/
│       ├── InnerUserService.java    #   @FeignClient("ai-code-user")
│       ├── InnerAppService.java     #   @FeignClient("ai-code-app")
│       ├── InnerSysLogService.java  #   @FeignClient("ai-code-log")
│       └── feign/                   #   BusinessErrorDecoder、FeignClientConfig
│
├── ai-code-ai/                      # AI 能力（JAR）
│   ├── .../ai/                      #   AiServices、路由、生成器
│   ├── .../ai/tools/                #   Function Calling：文件读写、搜图、Logo、构建
│   ├── .../config/                  #   模型 Bean（prototype）
│   └── resources/{prompt,skills}    #   提示词模板 / 技能
│
├── ai-code-app-service/             # 核心编排（:8125）
│   ├── .../controller/              #   App、ChatHistory、Statistics、Static、Health、InnerApp
│   ├── .../core/                    #   parser / saver / handler / builder
│   ├── .../rag/                     #   RAG 配置与切分器
│   └── .../service/impl/AppServiceImpl.java
│
├── ai-code-user-service/            # 用户服务（:8124）
│   └── .../controller/{User,InnerUser}Controller.java
│
├── ai-code-screenshot-service/      # 截图服务（:8126）
│   └── .../listener/GenScreenshotListener.java
│
├── ai-code-log-service/             # 日志服务（:8127）
│   └── .../listener/RecordLogListener.java
│
└── ai-code-gateway-service/         # 网关（:8100）
    └── resources/application-dev.yml  # 路由表在这里
```

---

## 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | 必须，`JAVA_HOME` 要指向 21 |
| Maven | 3.7+ | |
| MySQL | 8.x | |
| Redis | 6.x / 7.x | 用于 Session 共享与缓存 |
| RabbitMQ | 4.1.5 | 需开启管理台 |
| Nacos | 2.x | |
| Qdrant | 1.17 | 可选，未配置时回退 InMemory |
| Node.js | 20+ | 仅前端需要 |

---

## 快速开始

### 1. 启动基础设施

至少先跑起来：MySQL、Redis、RabbitMQ、Nacos、Qdrant（可选）。

RabbitMQ 需要建一个 vhost（示例用 `ai-code-platform`），并给账号授权；Exchange 与 Queue 由服务启动时自动声明。

### 2. 准备配置文件

> ⚠️ **`application-dev.yml` 在 `.gitignore` 里，仓库中并不存在。**
> clone 之后必须自己按 `application-pro.yml` 的结构补齐每个服务的 dev 配置，
> 否则启动会因缺少数据源 / 中间件地址而失败。

需要补齐的模块：

```
ai-code-user-service / ai-code-app-service / ai-code-log-service
ai-code-screenshot-service / ai-code-gateway-service
```

各服务 `application.yml` 里的 `spring.profiles.active` 默认就是 `dev`，
所以文件名必须是 `application-dev.yml`。

### 3. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS ai_code_platform DEFAULT CHARSET utf8mb4;
USE ai_code_platform;
SOURCE sql/create_table.sql;                    -- 全新建库
SOURCE sql/2026-10-19_add_user_email.sql;       -- 增量：user 表补 email 字段（老库才需要）
```

> 微服务版**不分主从**，所有服务连同一个库。
> 增量脚本对已执行过的库会报 1060/1061（列/索引已存在），属正常。

### 4. 编译

```bash
mvn -DskipTests install
```

> 必须 `install` 而不是 `package`：下面用 `-pl` 单独启动某个服务时，
> 它对 `ai-code-common` / `model` / `client` 的依赖是从**本地仓库**解析的，
> 只 `package` 的话会报 `Could not resolve dependencies`。

### 5. 启动服务（建议顺序）

```bash
# ① 注册中心里先有提供方，网关的 lb:// 才能解析
mvn -pl ai-code-user-service         spring-boot:run   # :8124
mvn -pl ai-code-log-service          spring-boot:run   # :8127
mvn -pl ai-code-screenshot-service   spring-boot:run   # :8126

# ② 核心编排服务
mvn -pl ai-code-app-service          spring-boot:run   # :8125

# ③ 最后起网关
mvn -pl ai-code-gateway-service      spring-boot:run   # :8100
```

> `ai-code-ai` 是 JAR，**不单独启动**。

### 6. 启动前端

```bash
cd ai-code-platform-frontend
npm install
npm run dev     # http://localhost:5173
```

Vite 的 `/api` 代理需指向**网关**：

```ts
// vite.config.ts
proxy: {
  '/api': { target: 'http://localhost:8100', changeOrigin: true },
}
```

### 7. 验证

```bash
curl http://localhost:8100/api/health              # app-service 健康检查
curl http://localhost:8100/api/user/get/login      # 预期 {"code":40100,"message":"未登录"}
```

- Knife4j 文档（各服务直连）：`http://localhost:8125/api/doc.html`
- Nacos 控制台：`http://<nacos-host>:8848/nacos`

---

## 关键配置与必填项

| 分类 | 配置项 | 说明 |
|---|---|---|
| 数据库 | `spring.datasource.url/username/password` | 所有服务同一个库 |
| Redis | `spring.data.redis.*` | Session 共享，三个服务必须用**同一个 namespace** |
| Session | `spring.session.redis.namespace` | 默认 `ai-code:session`，与单体版 `spring:session` 隔离 |
| MQ | `spring.rabbitmq.{host,port,virtual-host,username,password}` | |
| 注册中心 | `spring.cloud.nacos.discovery.server-addr` | 网关与四个服务都要配 |
| AI 模型 | `langchain4j.open-ai.*`（base-url / api-key / model-name） | 见下 |
| COS | `cos.client.{host,secretId,secretKey,region,bucket}` | 头像、聊天图片、应用封面 |
| 邮件 | `spring.mail.*` + `app.mail.from` | **password 填授权码，不是登录密码**；`from` 必须等于 `username` |
| 安全 | `GROK_API_KEY`、`CHATGPT_API_KEY` | 使用多模型时需要 |

---

## 核心功能

### 三种代码生成模式

| 模式 | 说明 |
|---|---|
| **HTML 单页** | 单文件输出，CSS/JS 内联 |
| **多文件项目** | HTML + CSS + JS 分离 |
| **Vue 工程** | 完整 Vue 3 + Vite 项目，AI 通过工具读写文件并自动构建 |

生成类型在**创建应用时确定**，之后不可切换。

### AI 能力

- **流式输出**：基于 `TokenStream`，前端 fetch 逐块渲染；支持中途取消（双通道：断连接 + 服务端 cancel 标记）
- **深度思考**：推理模型的过程单独渲染，可折叠
- **Function Calling**：文件读写/修改、`SearchImage`（必应搜图）、`GenerateLogoSvg`（Iconify 拼装）、`BuildVueProject`、`Exit`
- **自修复构建**：Vue 工程构建失败时把日志回传给模型，由模型改代码后重建
- **多模态输入**：图片与文本文件作为生成参考
- **会话记忆**：Redis 存储，按 appId 共享，切换模型不丢上下文；历史回放按 token 预算裁剪

### 管理功能

用户管理（注册登录 / 邮箱验证 / VIP / 额度）、应用管理（创建 / 对话 / 预览 / 下载 / 部署）、
模板广场、操作日志、数据统计。

---

## RAG 知识库

- **嵌入模型**：本地 `bge-small-zh-v1.5`（512 维），零延迟零成本
- **集合**：`code_embeddings`，余弦距离
- **切分策略**：

```
Vue 文件   → <template> / <script> / <style>
HTML 文件  → <header> / <section> / <footer>
多文件项目 → 每个文件一个语义单元
```

- **检索链路**：

```
用户消息 → 本地嵌入 → Qdrant 检索
  → ConditionalContentRetriever（ThreadLocal 动态开关）
  → DefaultContentInjector → LLM
```

---

## Skill 技能集成

通过 LangChain4j `Skills.toolProvider()` 按需注入，共 9 个技能：

| 技能 | 触发条件 |
|---|---|
| form-validation-patterns | 页面包含表单 |
| table-list-patterns | 列表 / 表格 |
| api-call-pattern | 涉及 API 请求 |
| responsive-breakpoints | 响应式布局 |
| design-tokens | 颜色 / 间距 / 阴影 |
| micro-interactions | 动画 / 过渡 |
| ui-reference | 上传参考图或 URL |
| code-audit | 生成后质量检查 |
| responsive-check | 多断点布局验证 |

> 技能由工具提供方自动挂载，用户侧不可开关。

---

## 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| 经网关访问返回 **503 Service Unavailable** | `lb://` 解析不到实例：服务未注册，或网关缺 `spring-cloud-starter-loadbalancer` | 确认 Nacos 里有该服务实例；确认网关依赖含 loadbalancer |
| 经网关访问返回 **404** | 网关路由把路径前缀配到了错误的服务 | 对照[网关路由表](#网关路由表)，`chatHistory/static/statistics` 属于 app-service |
| 返回 **40100** 但 HTTP 是 200 | 业务错误统一 HTTP 200 + `code`，这是正常约定 | 前端按 `code` 判断 |
| `ClassNotFoundException: com.ai.codeplatform.model.entity.User` | 拿到了**单体版**写进 Redis 的 Session | 清掉浏览器 Cookie 与旧 `spring:session:*`；确认 namespace 隔离生效 |
| `Public Key Retrieval is not allowed` | MySQL 8 `caching_sha2_password` 非 SSL 握手失败 | JDBC URL 加 `allowPublicKeyRetrieval=true&useSSL=false` |
| `Data too long for column 'operation'` | 异常信息超长导致整条日志写库失败 | 已在消费端截断至 80 字符；排查时应回看应用日志里的完整堆栈 |
| 前端请求 **500 且响应体为空** | Vite 代理连不上目标端口 | 检查 `vite.config.ts` 的 target 是否指向正在运行的网关 |

---

## License

[MIT License](LICENSE)
