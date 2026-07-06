# Jenkins Demo Format Spring Boot Starter 改造设计

**Date:** 2026-06-10
**Status:** Approved for Implementation

## 需求概述

将 `jenkins-demo-format` 项目改造为 Spring Boot Starter，使得：
1. 其他 Spring Boot 项目引入该依赖后可以访问内置的 Jenkins 流水线管理页面
2. 该项目可以单独启动运行
3. 前端编译文件放入 resources/static 文件夹
4. 数据库路径固定为 `D:\sqlite\sqlite.db`

## 约束条件

- **不允许修改前端代码** - 前端使用 `/api` 作为 baseURL，代理配置指向后端
- **不允许修改后端 Controller 的 API 地址** -保持 `/api/pipeline`、`/api/jenkins-config` 等现有路径
- **包名保持不变** - `com.example.jenkinsdemoformat`
- **数据库路径固定** - `D:\sqlite\sqlite.db`

##架构设计

### 组件结构

```
jenkins-demo-format/
├── pom.xml                                    # 改造为 starter POM
├── src/main/java/com/example/jenkinsdemoformat/
│   ├── JenkinsDemoFormatApplication.java # 保留 - 可独立运行
│   ├── JenkinsDemoFormatAutoConfiguration.java # 新增 - 自动配置
│   ├── JenkinsDemoFormatProperties.java      # 新增 - 配置属性
│   ├── config/WebMvcConfig.java              # 新增 - Web MVC 配置
│   └── ... (现有代码保持不变)
├── src/main/resources/
│   ├── static/                                # 新增 - 前端构建产物
│   │   ├── index.html
│   │   └── assets/
│   ├── application.yaml                       # 改造 - 数据库路径
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 自动配置机制

使用 Spring Boot 3.x 标准自动配置：
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 声明自动配置类
- `@MapperScan` 自动扫描 mapper
- 数据源自动配置（SQLite）
- WebMvc 静态资源处理

### 集成方式

其他 Spring Boot 项目引入依赖后自动获得：
- `/api/pipeline` - 流水线管理 API
- `/api/jenkins-config` - Jenkins 配置 API
- `/` 或 `/index.html` - 前端页面

## 文件变更清单

### 新增文件

1. `src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatProperties.java`
   - 配置属性类，prefix: `jenkins.demo-format`
   - 可配置项：API URL、用户名、Token 等

2. `src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatAutoConfiguration.java`
   - 自动配置类
   - 启用 `@MapperScan`、`数据源`、`WebMvc`

3. `src/main/java/com/example/jenkinsdemoformat/config/WebMvcConfig.java`
   - 配置静态资源路径 `/` → `classpath:/static/`
   -根路径重定向到 `/index.html`

4. `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - 声明自动配置类路径

5. `src/main/resources/static/**`
   - 前端构建产物（从 `pipeline-editor/dist/` 复制）

### 修改文件

1. `pom.xml`
   - 添加 `spring-boot-autoconfigure` 依赖
   - 添加 `spring-boot-autoconfigure-processor` 依赖
   - 保留所有现有依赖

2. `src/main/resources/application.yaml`
   - 数据库路径改为 `D:\sqlite\sqlite.db`

## 技术栈

- Spring Boot 3.2.5
- MyBatis-Plus 3.5.5
- SQLite 3.45.1.0
- springdoc-openapi 2.4.0
- Hutool 5.8.26
- Vue 3 + Element Plus (前端)

## 实施步骤

见 `docs/superpowers/plans/2026-06-09-spring-boot-starter-conversion.md`

## 验证方式

1. 独立运行：`mvn spring-boot:run` 启动成功
2. 构建验证：`mvn clean package` 生成可导入的 jar
3. API 验证：`curl http://localhost:9999/api/pipeline/pipelines` 返回流水线列表
4. 页面验证：访问 `http://localhost:9999/` 显示前端页面