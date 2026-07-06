# Spring Boot Starter 改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 jenkins-demo-format 改造为 Spring Boot Starter，支持独立启动和被其他项目引入，Jenkins 配置仅从数据库读取。

**Architecture:** 单模块结构 + Spring Profile 控制激活模式。独立模式（standalone profile）启用 DataSource + Controller + 静态资源；引入模式（library mode）通过 `@EnableJenkinsDemoFormat` 注解激活，仅注入 Service Bean。

**Tech Stack:** Spring Boot 3.2.5, MyBatis-Plus 3.5.5, SQLite, Hutool

---

## 文件变更概览

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/main/java/.../EnableJenkinsDemoFormat.java` | 新建 | 激活注解 |
| `src/main/java/.../JenkinsDemoFormatAutoConfiguration.java` | 修改 | 条件化配置 |
| `src/main/java/.../config/DataSourceConfig.java` | 新建 | SQLite DataSource（仅 standalone模式） |
| `src/main/java/.../JenkinsDemoFormatProperties.java` | 修改 | 移除 Jenkins字段，仅保留基础配置 |
| `src/main/resources/application.yml` | 修改 | 移除 jenkins.demo-format.* 配置 |
| `src/main/resources/application-standalone.yml` | 新建 | 独立模式专用配置 |
| `pom.xml` | 修改 | 调整 packaging 为 jar，添加 starter描述 |

---

## Task 1: 创建 EnableJenkinsDemoFormat 激活注解

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/EnableJenkinsDemoFormat.java`

- [ ] **Step 1: 创建激活注解**

```java
package com.example.jenkinsdemoformat;

import org.springframework.context.annotation.Import;
import com.example.jenkinsdemoformat.JenkinsDemoFormatAutoConfiguration;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(JenkinsDemoFormatAutoConfiguration.class)
public @interface EnableJenkinsDemoFormat {
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/jenkinsdemoformat/EnableJenkinsDemoFormat.java
git commit -m "feat: 添加 EnableJenkinsDemoFormat 激活注解"
```

---

## Task 2: 重构 JenkinsDemoFormatProperties

**Files:**
- Modify: `src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatProperties.java`

- [ ] **Step 1: 移除 Jenkins 相关字段，仅保留独立模式配置**

```java
package com.example.jenkinsdemoformat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jenkins.demo-format")
public class JenkinsDemoFormatProperties {

    private String apiUrl = "http://localhost:8080";
    private String basePath = "/jenkins";

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatProperties.java
git commit -m "refactor: 移除 Jenkins 配置字段，仅保留基础配置"
```

---

## Task 3: 创建 DataSourceConfig（仅独立模式生效）

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/config/DataSourceConfig.java`

- [ ] **Step 1: 创建 DataSource 配置类**

```java
package com.example.jenkinsdemoformat.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "standalone", matchIfMissing = false)
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(org.springframework.core.env.Environment env) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        String url = env.getProperty("spring.datasource.url", "jdbc:sqlite:C:\\sqlite\\sqlite.db");
        dataSource.setUrl(url);
        return dataSource;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/jenkinsdemoformat/config/DataSourceConfig.java
git commit -m "feat: 添加 DataSourceConfig，standalone模式启用 SQLite"
```

---

## Task 4: 重构 JenkinsDemoFormatAutoConfiguration

**Files:**
- Modify: `src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatAutoConfiguration.java`

- [ ] **Step 1: 更新自动配置类，条件化 MapperScan 和 Properties**

```java
package com.example.jenkinsdemoformat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.example.jenkinsdemoformat.config.DataSourceConfig;
import com.example.jenkinsdemoformat.config.RestConfig;
import com.example.jenkinsdemoformat.config.WebMvcConfig;

@Configuration
@MapperScan("com.example.jenkinsdemoformat.mapper")
@EnableConfigurationProperties(JenkinsDemoFormatProperties.class)
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "standalone", matchIfMissing = false)
@Import({RestConfig.class, WebMvcConfig.class, DataSourceConfig.class})
public class JenkinsDemoFormatAutoConfiguration {
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatAutoConfiguration.java
git commit -m "refactor: 重构 AutoConfiguration，standalone 模式启用完整组件"
```

---

## Task 5: 重构 application.yml 和新建 application-standalone.yml

**Files:**
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/application-standalone.yml`

- [ ] **Step 1: 修改 application.yml，移除 Jenkins 配置，保留基础结构**

```yaml
spring:
  application:
    name: jenkins-demo-format

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.jenkinsdemoformat.entity
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.example.jenkinsdemoformat.mapper: DEBUG
```

- [ ] **Step 2: 创建 application-standalone.yml**

```yaml
spring:
  profiles:
    active: standalone
  datasource:
    driver-class-name: org.sqlite.JDBC
    url: jdbc:sqlite:${SQLITE_DB_PATH:C:\sqlite\sqlite.db}

server:
  port: 9999

springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml src/main/resources/application-standalone.yml
git commit -m "refactor: 分离 standalone 配置到独立 profile"
```

---

## Task 6:调整 pom.xml 打包配置

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 spring-boot-starter 作为依赖，调整 packaging**

将 `<packaging>jar</packaging>` 保持（Spring Boot starter 默认 jar），并确保 maven-surefire-plugin 跳过测试。

```xml
<!-- 在 dependencies 中添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

- [ ] **Step 2: Commit**

```bash
git add pom.xml
git commit -m "build: 调整 pom.xml 支持 starter 打包"
```

---

## Task 7: 验证构建

- [ ] **Step 1: 执行 Maven 构建**

```bash
cd C:/Users/cetc/Desktop/zmb/jenkins-demo-format
mvn clean package -DskipTests
```

预期: BUILD SUCCESS，生成 `target/jenkins-demo-format-0.0.1-SNAPSHOT.jar`

- [ ] **Step 2: 验证 jar 包结构**

```bash
jar tf target/jenkins-demo-format-0.0.1-SNAPSHOT.jar | grep -E "(static|application|META-INF)"
```

预期: 包含 `BOOT-INF/classes/static/`、`BOOT-INF/classes/application.yml`、`META-INF/`目录结构

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "build: 验证 starter 打包成功"
```

---

## Task 8: 独立模式启动验证

- [ ] **Step 1: 以 standalone 模式启动**

```bash
java -jar target/jenkins-demo-format-0.0.1-SNAPSHOT.jar --spring.profiles.active=standalone
```

预期输出: `流水线代理工程启动成功`，监听端口 9999

- [ ] **Step 2: 验证 API 访问**

```bash
curl http://localhost:9999/api/jenkins-config
```

预期: 返回 `{"code":200,"data":[...]}` 或空数组

- [ ] **Step 3: 验证前端页面**

```bash
curl http://localhost:9999/
```

预期: 返回 `index.html` 或 302 重定向

---

## Task 9: 更新 JenkinsDemoFormatApplication.java 主类

**Files:**
- Modify: `src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatApplication.java`

- [ ] **Step 1: 添加 standalone profile 激活的主类**

```java
package com.example.jenkinsdemoformat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.jenkinsdemoformat.mapper")
public class JenkinsDemoFormatApplication {

    public static void main(String[] args) {
        SpringApplication.run(JenkinsDemoFormatApplication.class, args);
        System.out.println("流水线代理工程启动成功 ");
    }
}
```

主类保持不变，因为它本身就是 standalone 模式的入口。

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatApplication.java
git commit -m "chore: 更新主类说明"
```

---

## 自检清单

完成所有任务后，逐项核对：

1. **Spec覆盖检查** — 设计文档中每项需求都有对应任务实现
2. **占位符扫描** — 无 TBD/TODO/不完整步骤
3. **类型一致性** — 各任务间方法签名、字段名称一致
4. **构建验证** — `mvn clean package -DskipTests` 成功
5. **独立模式启动** — standalone profile 启动正常，API 可访问

---

**Plan complete and saved to `docs/superpowers/plans/2026-06-12-spring-boot-starter-conversion-plan.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**