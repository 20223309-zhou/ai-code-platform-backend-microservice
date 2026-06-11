# AI Code Platform

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0-6DB33F?logo=spring)](https://spring.io/projects/spring-cloud)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.15-000000?logo=langchain)](https://docs.langchain4j.dev)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-4FC08D?logo=vuedotjs)](https://vuejs.org/)
[![Qdrant](https://img.shields.io/badge/Qdrant-EE3664?logo=qdrant)](https://qdrant.tech/)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)](https://redis.io/)
[![Dubbo](https://img.shields.io/badge/Dubbo-3.3-FF6A00?logo=apache)](https://dubbo.apache.org/)

> 基于 LangChain4j 的 AI 代码生成平台，支持自然语言生成 HTML / 多文件 / Vue 工程代码，具备 RAG 知识库检索、Skill 技能按需注入、多模态文件上传、流式输出与实时预览等能力。采用微服务架构，通过 Spring Cloud + Dubbo 实现服务治理。

---

## 仓库

- **后端仓库（单体）**：[https://github.com/20223309-zhou/ai-code-platform-backend](https://github.com/20223309-zhou/ai-code-platform-backend)
- **前端仓库**：[https://github.com/20223309-zhou/ai-code-platform-frontend](https://github.com/20223309-zhou/ai-code-platform-frontend)

---

## 项目架构

```
ai-code-platform/
├── ai-code-ai                  # AI 服务：LLM 调用、AiServices、工具调用、RAG 检索
├── ai-code-app-service         # 应用服务：应用 CRUD、部署、代码生成编排
├── ai-code-client              # 前端（Vue 3 + TypeScript）
├── ai-code-common              # 公共模块：常量、工具类、基础配置
├── ai-code-model               # 数据模型：实体类、VO、DTO、枚举
├── ai-code-screenshot-service  # 截图服务：网页截图生成与上传
├── ai-code-user-service        # 用户服务：用户管理、GitHub OAuth 登录
├── sql/                        # 数据库初始化脚本
└── tmp/                        # 生成的代码文件与部署目录
```

| 服务 | 端口 | 说明 |
|------|------|------|
| ai-code-user-service | 8124 | 用户认证、OAuth2 登录 |
| ai-code-app-service | 8125 | 应用管理、代码生成、静态资源 |
| ai-code-ai |  | AI 模型调度、RAG 检索、工具执行 |
| ai-code-screenshot-service | 8126 | 网页截图生成 |
| ai-code-frontend | 5173 | Vue 3 前端页面 |

---

## 技术栈

### 后端

| 技术 | 说明 |
|------|------|
| **Java 21** | 使用虚拟线程（Virtual Threads）处理高并发 |
| **Spring Boot 3.5** | 微服务基础框架 |
| **Spring Cloud 2023.0** | 服务注册与配置管理 |
| **Apache Dubbo 3.3** | 服务间 RPC 调用（Triple 协议）|
| **Nacos** | 注册中心与配置中心 |
| **LangChain4j 1.15** | AI 服务编排、工具调用、RAG、Skills |
| **MyBatis-Flex** | ORM 框架 |
| **MySQL 8** | 业务数据库 |
| **Redis 7** | 分布式 Session 共享 |
| **Qdrant** | 向量数据库（存储代码向量） |
| **腾讯云 COS** | 图片与文件存储 |

### 前端

| 技术 | 说明 |
|------|------|
| **Vue 3.5** | 前端框架 |
| **TypeScript 5.8** | 类型安全 |
| **Vite 7** | 构建工具 |
| **Ant Design Vue** | UI 组件库 |
| **Pinia** | 状态管理 |

---

## 核心功能

### 三种代码生成模式

| 模式 | 说明 | 技术实现 |
|------|------|---------|
| **HTML 单页** | 单文件生成，CSS/JS 内联 | TokenStream + 代码块解析 |
| **多文件项目** | HTML + CSS + JS 分离 | TokenStream + 多文件解析 |
| **Vue 工程** | 完整 Vue 3 + Vite 项目 | TokenStream + 工具调用（readFile / writeFile / modifyFile）|

### AI 能力

- **多模型支持**：DeepSeek V4 Flash（推理）、Qwen-turbo（路由分类）、Qwen-max（SVG Logo 生成）
- **流式输出**：三种模式均基于 TokenStream 流式输出，前端逐字渲染
- **深度思考**：支持 DeepSeek 推理过程的前端流式展示
- **工具调用**：SearchImageTool、GenerateLogoSvg、WebFetchTool、FileReadTool、FileModifyTool、FileWriteTool、SearchIconTool 等
- **多模态输入**：支持图片和文本文件上传作为生成参考

### 在线体验

- **流式预览**：代码生成过程中实时更新预览
- **可视化编辑**：点击选中页面元素，描述修改需求
- **一键部署**：生成的项目可部署到线上，提供访问地址
- **模板广场**：精选优质模板，一键复用

---

## RAG 知识库

### 嵌入模型

采用 **BAAI/bge-small-zh-v1.5**（512 维）作为本地嵌入模型，零延迟、零成本。

### 代码切分策略

不按 token 数机械截断，而是按代码的**语义结构**切分：

```
Vue 文件   → <template> / <script> / <style>
HTML 文件  → <header> / <section> / <footer>
多文件项目 → 每个文件为一个语义单元
```

### 检索链路

```
用户消息 → BgeSmallZhV15 嵌入 → Qdrant 余弦检索
  → ConditionalContentRetriever（ThreadLocal 动态开关）
  → DefaultContentInjector（拼接上下文）
  → LLM
```

### 去重机制

部署时增量索引，通过零向量搜索检测 Qdrant 中已存在的文件路径，避免重复嵌入。

---

## Skill 技能集成

LangChain4j `Skills.toolProvider()` 机制，9 个按需激活的技能：

| 技能 | 触发条件 |
|------|---------|
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

## GitHub OAuth 登录

支持 GitHub OAuth 2.0 登录，首次登录自动创建账号，后续登录自动更新头像和昵称。会话基于 Redis 分布式 Session，支持多实例部署。

---

## 许可证

[MIT License](LICENSE)