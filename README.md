# iCode — AI 驱动的应用生成平台（微服务架构）

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring_Cloud_Alibaba-2023.0.1.0-orange?logo=spring)](https://spring.io/projects/spring-cloud)
[![Apache Dubbo](https://img.shields.io/badge/Dubbo-3.3.0-FB6F01?logo=apachedubbo)](https://dubbo.apache.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-异步消息-FF6600?logo=rabbitmq)](https://www.rabbitmq.com/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.15-000000?logo=langchain)](https://docs.langchain4j.dev)
[![MyBatis-Flex](https://img.shields.io/badge/MyBatis_Flex-1.11-FF6A00?logo=mybatis)](https://mybatis-flex.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)](https://redis.io/)
[![Qdrant](https://img.shields.io/badge/Qdrant-向量数据库-EE3664?logo=qdrant)](https://qdrant.tech/)

> 基于 LangChain4j 的 AI 代码生成平台，支持自然语言生成 HTML / 多文件 / Vue 工程代码，具备 RAG 知识库检索、Skill 技能按需注入、多模态文件上传、流式输出与实时预览等能力。采用**微服务架构**，通过 Spring Cloud Alibaba + Dubbo 实现服务治理，RabbitMQ 实现异步解耦。

---

## 仓库地址

后端仓库（单体）：https://github.com/20223309-zhou/ai-code-platform-backend

前端仓库：https://github.com/20223309-zhou/ai-code-platform-frontend

## 模块概览

| 模块 | 说明 | 角色 |
|---|---|---|
| **ai-code-common** | 公共模块：工具类、异常体系、常量、基础配置、COS 对象存储 | JAR 包 |
| **ai-code-model** | 数据模型：Entity、DTO、VO、枚举，所有模块共享的数据结构 | JAR 包 |
| **ai-code-client** | 内部服务接口定义：Dubbo 服务接口定义 | JAR 包 |
| **ai-code-user-service** | 用户服务：注册登录、VIP 等级、额度管理 | Nacos 提供者 |
| **ai-code-ai** | AI 服务：LLM 调度、Function Calling 工具、RAG 检索、提示词管理 | Nacos 提供者 |
| **ai-code-app-service** | 应用服务：代码生成编排、对话管理、模板广场、统计 | Nacos 消费者 + MQ 生产者 |
| **ai-code-screenshot-service** | 截图服务：通过 MQ 消费截图请求，使用 Selenium 截图并上传 COS | MQ 消费者 |
| **ai-code-log-service** | 日志服务：通过 MQ 消费日志写入数据库，同时提供 REST 查询 API | MQ 消费者 + REST API |

---

## 架构设计

```
┌─────────────────────────────────────────────────────────────────────────┐
│                             客户端 (Vue 3)                              │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ HTTP / WebSocket
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          API 网关 (待接入)                               │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  ai-code-user-   │  │  ai-code-app-    │  │  ai-code-ai      │
│  service         │  │  service         │  │                  │
│  (提供者)         │◄─┤  (消费者)         │──►│  (提供者)        │
│  端口 8124       │  │  端口 8125        │   │  内嵌依赖         │
└──────────────────┘  └──────┬───────────┘  └──────────────────┘
                             │
                     ┌───────┴───────────────────────────────┐
                     │          RabbitMQ                     │
                     │  ┌──────────────────┐                 │
                     │  │ log.topic        │                 │
                     │  ├ log.record ──────┼──► log.queue     │
                     │  │ screenshot.create│──► screenshot.queue │
                     │  └──────────────────┘                  │
                     └───────┬───────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐
  │  ai-code-      │  │  ai-code-      │  │  未来扩展服务    │
  │  log-service   │  │  screenshot-   │  │                │
  │  (MQ 消费者)    │  │  service       │  │                │
  │  端口 8127      │  │  (MQ 消费者)    │  │                │
  └────────────────┘  └────────────────┘  └────────────────┘
```

### 通信模式

| 方式 | 场景 | 实现 |
|---|---|---|
| **Dubbo RPC** | 服务间同步调用（User → App、AI → App） | `ai-code-client` 定义接口，Nacos 服务发现 |
| **RabbitMQ** | 异步解耦任务（App → Log 日志、App → Screenshot 截图） | Topic Exchange + 独立队列 |

---

## 消息队列设计

采用 **Topic Exchange** 模式，按业务类型分发消息，确保每个消息只被消费一次：

```
                              Topic Exchange
                         ┌──────────────────┐
                         │   code.exchange   │
                         └────────┬─────────┘
                                  │
                  ┌───────────────┼────────────────┐
                  │               │                │
          routingKey="log.record" │  routingKey="screenshot.create"
                  │               │                │
          ┌───────▼──────┐  ┌────▼─────┐  ┌──────▼─────────┐
          │  log.queue   │  │ (预留)    │  │ screenshot.queue│
          │  concurrency │  │          │  │  concurrency=2  │
          │  =2          │  │          │  │                 │
          └───────┬──────┘  └──────────┘  └──────┬───────── ┘
                  │                              │
          ┌───────▼──────┐              ┌────────▼────────┐
          │ Log 服务      │              │ Screenshot 服务  │
          │ 写入数据库     │              │ 截图 + 上传 COS  │
          └──────────────┘              └─────────────────┘
```

- **日志队列**：`concurrency=2` + `prefetch=1`，两个消费者竞争消费，实现公平分发
- **截图队列**：单消费者，截图任务较重（Selenium 渲染），避免过度竞争
- **消息持久化** + **手动 ACK**：确保消息不丢失，消费者崩溃后自动重投

---

## 模块依赖关系

```
ai-code-common  ──── 基础工具、异常、常量、安全、COS
      │
      ▼
ai-code-model  ──── Entity / DTO / VO / 枚举
      │
      ▼
ai-code-client  ──── Dubbo 接口定义
      │
      ├────────────────┬─────────────────┐
      ▼                ▼                 ▼
ai-code-user-service   ai-code-ai        ai-code-app-service
(Nacos 提供者)          (Nacos 提供者)     (Nacos 消费者)
      │                                    │
      └────────────── Dubbo ───────────────┘
                                           │
                             ┌─────────────┴─────────────┐
                             ▼                           ▼
                   ai-code-log-service      ai-code-screenshot-service
                   (MQ 消费者 + REST API)    (MQ 消费者)
```

---

## 技术栈

### 后端

| 技术 | 版本         | 用途 |
|---|------------|---|
| Java | 21         | 开发语言 |
| Spring Boot | 3.5.3      | 微服务基础框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 微服务治理 |
| Apache Dubbo | 3.3.0      | 服务间 RPC 调用（Triple 协议） |
| Nacos | 2.x        | 注册中心与配置中心 |
| RabbitMQ | 4.1.5      | 异步消息（日志、截图） |
| MyBatis-Flex | 1.11.0     | ORM 框架 |
| MySQL | 8.x        | 业务数据库 |
| Redis | 7.x        | 分布式 Session 共享 + 缓存 |
| LangChain4j | 1.15.0     | AI 服务编排、工具调用、RAG、Skills |
| Qdrant | 1.17       | 向量数据库（存储代码向量） |
| Redisson | 3.50.0     | 分布式限流 |
| Selenium | 4.33.0     | 网页截图 |
| 腾讯云 COS | 5.6.227    | 对象存储 |

### 前端

| 技术 | 说明 |
|---|---|
| **Vue 3.5** | 前端框架 |
| **TypeScript 5.8** | 类型安全 |
| **Vite 7** | 构建工具 |
| **Ant Design Vue** | UI 组件库 |
| **Pinia** | 状态管理 |

### AI 模型

| 用途 | 模型 |
|---|---|
| 代码生成 / 工具调用 | DeepSeek `deepseek-v4-flash` |
| 路由分类 / 意图识别 | Qwen-turbo |
| SVG Logo 生成 | Qwen-max |
| 嵌入模型 | BAAI bge-small-zh-v1.5（512 维） |

---

## 项目结构

```text
ai-code-platform-microsoft-lately/
├── pom.xml                              # 父 POM（聚合 8 个子模块）
├── mvnw / mvnw.cmd / .mvn/              # Maven Wrapper
├── sql/                                 # 数据库建表脚本
├── tmp/                                 # 运行时生成代码与截图（git ignored）
│
├── ai-code-common/                      # 公共模块
│   └── src/main/java/com/ai/codeplatform/
│       ├── common/                      #   统一返回、分页请求
│       ├── config/                      #   基础安全、CORS、COS、JSON
│       ├── constant/                    #   常量
│       ├── exception/                   #   异常体系
│       ├── annotation/                  #   注解 (@AuthCheck, @LogRecord)
│       ├── aop/                         #   基础 AOP 配置
│       ├── manager/                     #   管理器（COS、取消生成）
│       ├── generator/                   #   MyBatis 代码生成器
│       ├── utils/                       #   工具类
│       └── Interceptor/                 #   拦截器（RAG 开关）
│
├── ai-code-model/                       # 数据模型模块
│   └── src/main/java/com/ai/codeplatform/model/
│       ├── entity/                      #   数据库实体（App、User、ChatHistory 等）
│       ├── dto/                         #   请求 DTO（增删改查）
│       ├── vo/                          #   视图对象
│       └── enums/                       #   枚举
│
├── ai-code-client/                      # 内部服务接口
│   └── src/main/java/com/ai/codeplatform/innerservice/
│       ├── InnerUserService.java        #   用户 Dubbo 接口
│       ├── InnerAppService.java         #   应用 Dubbo 接口
│       └── InnerSysLogService.java      #   日志 Dubbo 接口
│
├── ai-code-user-service/                # 用户服务（端口 8124）
│   ├── .../controller/UserController.java
│   ├── .../service/UserService.java
│   ├── .../service/impl/InnerUserServiceImpl.java
│   └── .../mapper/UserMapper.java
│
├── ai-code-ai/                          # AI 服务
│   ├── .../ai/                          #   AiServices、路由、生成器
│   ├── .../ai/tools/                    #   Function Calling 工具（文件读写、网页获取等）
│   ├── .../config/                      #   模型配置
│   └── src/main/resources/
│       ├── prompt/                      #   提示词模板（5 种生成模式）
│       └── skills/                      #   9 个按需激活的技能
│
├── ai-code-app-service/                 # 应用服务 —— 核心编排（端口 8125）
│   ├── .../controller/                  #   App、ChatHistory、Statistics
│   ├── .../core/                        #   代码生成编排、解析、保存、构建
│   │   ├── builder/                     #   Vue 项目构建
│   │   ├── handler/                     #   流式消息处理
│   │   ├── parser/                      #   代码解析（HTML/多文件）
│   │   └── saver/                       #   代码保存（本地/COS）
│   ├── .../rag/                         #   RAG 知识库配置
│   ├── .../ratelimit/                   #   分布式限流（Redisson）
│   └── .../service/                     #   业务服务实现
│       └── impl/AppServiceImpl.java
│
├── ai-code-screenshot-service/          # 截图服务（端口 8126）
│   ├── .../listener/GenScreenshotListener.java   #   MQ 消费者
│   ├── .../service/ScreenshotService.java
│   └── .../utils/WebScreenshotUtils.java
│
└── ai-code-log-service/                 # 日志服务（端口 8127）
    ├── .../listener/RecordLogListener.java       #   MQ 消费者
    ├── .../controller/SysOperationLogController.java
    ├── .../service/SysOperationLogService.java
    └── .../mapper/SysOperationLogMapper.java
```

---

## 环境要求

| 组件 | 版本要求                    |
|---|-------------------------|
| JDK | 21                      |
| Maven | 3.7+                    |
| MySQL | 8.x                     |
| Redis | 6.x / 7.x               |
| RabbitMQ | 4.1.5                   |
| Nacos | 2.x                     |
| Qdrant | 1.17（可选，可用 InMemory 回退） |

---

## 快速启动

### 1. 基础设施

确保以下服务已启动：
- MySQL（端口 3306）
- Redis（端口 6379）
- RabbitMQ（端口 5672，管理面板 15672）
- Nacos（端口 8848）

### 2. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS ai_code_platform;
source sql/create_table.sql;
```

### 3. 启动顺序（按依赖关系）

```bash
# Step 1: 编译全部模块（common → model → client → 各服务）
mvn clean package -DskipTests

# Step 2: 启动 Nacos 提供者
mvn -pl ai-code-user-service spring-boot:run         # 端口 8124
mvn -pl ai-code-ai spring-boot:run                   # 内嵌依赖

# Step 3: 启动 MQ 消费者
mvn -pl ai-code-log-service spring-boot:run          # 端口 8127
mvn -pl ai-code-screenshot-service spring-boot:run   # 端口 8126

# Step 4: 启动核心消费者
mvn -pl ai-code-app-service spring-boot:run          # 端口 8125
```

### 4. 前端启动

```bash
cd ai-code-platform-frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

---

## 配置说明

每个服务模块的 `src/main/resources/` 下包含三套配置文件：

| 文件 | 环境 |
|---|---|
| `application.yml` | 通用默认配置 |
| `application-dev.yml` | 开发环境 |
| `application-pro.yml` | 生产环境 |

### 关键配置项

- **各服务的 MySQL 数据源**：地址、用户名、密码
- **Redis 连接**：地址、端口、密码
- **RabbitMQ 连接**：host、port、virtual-host
- **Nacos 注册中心**：server-addr
- **AI 模型**：base-url、api-key、model-name
- **腾讯云 COS**：secretId、secretKey、region、bucket

---

## 核心功能

### 三种代码生成模式

| 模式 | 说明 |
|---|---|
| **HTML 单页** | 单文件生成，CSS/JS 内联 |
| **多文件项目** | HTML + CSS + JS 文件分离 |
| **Vue 工程** | 完整 Vue 3 + Vite 项目，AI 工具调用读写文件 |

### AI 能力

- **多模型支持**：DeepSeek V4 Flash（推理）、Qwen-turbo（路由分类）、Qwen-max（Logo 生成）
- **流式输出**：基于 TokenStream，前端逐字渲染
- **深度思考**：支持推理过程的前端流式展示
- **Function Calling 工具**：SearchImageTool、GenerateLogoSvg、WebFetchTool、FileReadTool、FileModifyTool、FileWriteTool 等
- **RAG 知识库**：已部署代码自动入库，按文件类型（Vue/HTML/JS/CSS）语义切分，Qdrant 向量检索
- **Skill 技能系统**：9 个按需激活的设计规范与最佳实践技能
- **多模态输入**：支持图片和文本文件上传作为生成参考

### 管理功能

- **用户管理**：注册登录、VIP 等级、使用额度
- **应用管理**：创建、对话、预览、下载、部署
- **模板广场**：精选模板，一键复用
- **操作日志**：后台查询与管理
- **数据统计**：创作趋势、成功率、活跃用户

---

## RAG 知识库

### 嵌入模型

采用 **BAAI/bge-small-zh-v1.5**（512 维）本地嵌入模型，零延迟、零成本。

### 代码切分策略

```
Vue 文件   → <template> / <script> / <style>
HTML 文件  → <header> / <section> / <footer>
多文件项目 → 每个文件为一个语义单元
```

### 检索链路

```
用户消息 → 本地嵌入 → Qdrant 余弦检索
  → ConditionalContentRetriever（ThreadLocal 动态开关）
  → DefaultContentInjector（拼接上下文）→ LLM
```

---

## Skill 技能集成

LangChain4j `Skills.toolProvider()` 机制，9 个按需激活的技能：

| 技能 | 触发条件 |
|---|---|
| form-validation-patterns | 页面包含表单 |
| table-list-patterns | 需要展示列表/表格 |
| api-call-pattern | 涉及 API 请求 |
| responsive-breakpoints | 响应式布局 |
| design-tokens | 定义颜色/间距/阴影 |
| micro-interactions | 动画/过渡反馈 |
| ui-reference | 用户上传参考图片或 URL |
| code-audit | 代码生成完成后质量检查 |
| responsive-check | 验证多断点布局表现 |

---

## License

[MIT License](LICENSE)
