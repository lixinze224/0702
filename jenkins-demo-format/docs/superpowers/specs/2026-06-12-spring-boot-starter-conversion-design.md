# Jenkins Demo Format Spring Boot Starter 改造设计

## 1. 需求概述

将 `jenkins-demo-format` 工程改造为 Spring Boot Starter，满足以下需求：

- 其他 Spring Boot 项目引入 jar 包后可直接访问该项目的 API 和前端页面
- 该项目可独立启动运行
- Jenkins 配置信息全部从 SQLite 数据库读取，不从配置文件读取
- 前端编译文件内置于 resources/static 目录，随 jar 包一起打包

## 2. 激活模式

项目支持两种激活模式，通过 Spring Profile 控制：

| 模式 | 激活方式 | 效果 |
|------|---------|------|
| **独立模式 (standalone)** | `spring.profiles.active=standalone` | DataSource 创建生效、MapperScan、静态资源、API 全部可用 |
| **引入模式 (library)** | 消费方在配置类加 `@EnableJenkinsDemoFormat` | 仅注入 Service Bean，不创建 DataSource |

### 独立模式激活

消费方在自己的 `application.yml` 中指定 `spring.profiles.active=standalone` 或在启动参数中指定。

### 引入模式激活

消费方在主配置类或单独的配置类上加 `@EnableJenkinsDemoFormat` 注解：

```java
@EnableJenkinsDemoFormat
@SpringBootApplication
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
```

## 3. 项目结构

```
jenkins-demo-format
├── pom.xml # 打包为 spring-boot-starter
├── src/main/java/com/example/jenkinsdemoformat/
│   ├── JenkinsDemoFormatApplication.java     # 独立模式启动入口
│   ├── JenkinsDemoFormatAutoConfiguration.java  # 自动配置类
│   ├── EnableJenkinsDemoFormat.java           # 激活注解
│   ├── config/
│   │   ├── RestConfig.java                    # RestTemplate 配置
│   │   ├── WebMvcConfig.java                  # 前端静态资源 + API 前缀
│   │   └── DataSourceConfig.java              # SQLite DataSource 配置（仅独立模式）
│   ├── controller/
│   │   ├── PipelineController.java           # /api/pipeline/**
│   │   └── JenkinsConfigController.java      # /api/jenkins-config/**
│   ├── service/impl/
│   │   └── JenkinsServiceImpl.java # Jenkins API 调用实现
│   ├── mapper/
│   │   ├── JenkinsConfigMapper.java
│   │   └── PipelineMapper.java
│   └── entity/
│       ├── JenkinsConfig.java # jenkins_config 表
│       └── Pipeline.java                      # pipeline 表
├── src/main/resources/
│   ├── application.yml                        # 基础配置（不含 Jenkins IP）
│   ├── application-standalone.yml # 独立模式专用配置
│   └── static/                               # 前端编译文件
│       ├── index.html
│       └── assets/
└── src/main/META-INF/
    └── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 4. Jenkins 信息获取

所有 Jenkins IP/username/password/token 信息只从数据库读取，不再从配置文件读取 `jenkins.demo-format.*` 相关配置。

**`JenkinsDemoFormatProperties`** 仅保留独立模式的基础配置（如端口等），移除 Jenkins 连接相关字段。

## 5. Jenkins 实例路由

Pipeline 表通过 `jenkins_config_id` 字段关联到 `jenkins_config` 表。

消费方调用 API 时传入 `pipeLineId`，系统通过 Pipeline 找到关联的 JenkinsConfig，再执行 Jenkins API 调用。

## 6. 数据库路径配置

SQLite 数据库路径通过配置指定，支持消费方自定义路径：

```yaml
# application-standalone.yml
spring:
  datasource:
    url: jdbc:sqlite:${SQLITE_DB_PATH:C:\sqlite\sqlite.db}
```

消费方可通过环境变量 `SQLITE_DB_PATH` 或 JVM 参数 `-DSQLITE_DB_PATH=` 指定路径。

## 7. 前端静态文件

前端编译文件放置在 `src/main/resources/static/` 目录，随 jar 包一起打包。

消费方引入 jar 后通过 `http://host:port/` 直接访问前端页面。

## 8. 关键改动点

1. **JenkinsDemoFormatProperties** — 移除 Jenkins IP/username/token 字段，仅保留独立模式配置
2. **EnableJenkinsDemoFormat 注解** — 新建激活注解
3. **JenkinsDemoFormatAutoConfiguration** — 条件化配置，根据 Profile 和注解决定生效范围
4. **DataSourceConfig** — 新建配置类，仅在 standalone 模式创建 DataSource
5. **JenkinsServiceImpl** — 所有方法增加 `Long pipelineId` 参数，通过 PipelineMapper 获取关联的 JenkinsConfig
6. **PipelineController** — API 参数中增加 `pipeLineId`，路由到对应 Jenkins 实例
7. **application.yml** — 移除 `jenkins.demo-format.*` 配置
8. **application-standalone.yml** — 新建独立模式专用配置
9. **pom.xml** — 调整打包配置，支持作为 starter 使用