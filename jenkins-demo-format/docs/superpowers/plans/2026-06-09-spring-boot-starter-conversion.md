# Jenkins Demo Format Spring Boot Starter 改造计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 jenkins-demo-format 项目改造为 Spring Boot Starter，使得其他 Spring Boot 项目引入该依赖后可以访问内置的 Jenkins流水线管理页面。

**Architecture:**
-改造 pom.xml，移除内嵌 jar依赖，改用 Spring Boot Starter 标准结构
- 创建 auto-configuration 模块，自动配置数据库、Web MVC、API 路由
- 将前端页面（HTML/CSS/JS）放入 `src/main/resources/static` 目录
- 配置静态资源路径，使得引入该 starter 的项目可通过 `/jenkins/*` 访问页面

**Tech Stack:** Spring Boot 3.2.5, MyBatis-Plus, SQLite, springdoc-openapi

---

## 文件结构规划

```
jenkins-demo-format/
├── pom.xml                          # 改造为 starter POM
├── src/main/java/.../
│   ├── jenkins/
│   │   └── demo/
│   │       └── format/              # 重构包名
│   │           ├── JenkinsDemoFormatAutoConfiguration.java  # 自动配置类
│   │           ├── JenkinsDemoFormatProperties.java         # 配置属性类
│   │           └── WebMvcConfig.java                        # MVC 配置
│   └── ... # 保留原有 Java 代码
└── src/main/resources/
    ├── application.yaml              # 保留原有配置
    ├── static/                       # 前端静态资源 (新建)
    │   ├── index.html
    │   ├── css/
    │   └── js/
    └── templates/                    # HTML 模板 (可选)
```

---

## Task 1: 重构 pom.xml 为 Spring Boot Starter

**Files:**
- Modify: `pom.xml`

**Steps:**

- [ ] **Step 1:备份并重写 pom.xml**

将项目改为 starter POM，添加 `spring-boot-autoconfigure-processor` 依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.jenkins</groupId>
    <artifactId>jenkins-demo-format-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>Jenkins Demo Format Starter</name>
    <description>Spring Boot Starter for Jenkins Pipeline Management</description>

    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.2.5</spring-boot.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <springdoc.version>2.4.0</springdoc.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot Starter Auto Configure -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Springdoc OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>
        
        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        
        <!-- SQLite -->
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.45.1.0</version>
        </dependency>
        
        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.26</version>
        </dependency>
        
        <!-- Gson -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
        </dependency>
        
        <!-- Jenkins Core -->
        <dependency>
            <groupId>org.jenkins-ci.main</groupId>
            <artifactId>jenkins-core</artifactId>
            <version>2.462.1</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/lib/jenkins-core-2.462.1.jar</systemPath>
        </dependency>
        <!-- 其他 Jenkins依赖... -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Task 2: 重构包结构 (可选)

**Files:**
- Modify: 所有 Java 文件的包名从 `com.example.jenkinsdemoformat` 改为 `com.jenkins.demofmt`
- Modify: `JenkinsDemoFormatApplication.java` 移动到新包

**Steps:**

- [ ] **Step 1: 创建新包目录**

```bash
mkdir -p src/main/java/com/jenkins/demofmt/{common,config,controller,dto,entity,mapper,service,util}
```

- [ ] **Step 2: 移动并修改所有 Java 文件包名**

使用 IDE 重构或手动修改所有文件。

---

## Task 3: 创建配置属性类

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatProperties.java`

**Steps:**

- [ ] **Step 1: 创建配置属性类**

```java
package com.example.jenkinsdemoformat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jenkins.demo-format")
public class JenkinsDemoFormatProperties {
    
    private String apiUrl = "http://localhost:8080";
    private String username;
    private String token;
    private String basePath = "/jenkins";
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getBasePath() {
        return basePath;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
```

---

## Task 4: 创建自动配置类

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/JenkinsDemoFormatAutoConfiguration.java`
- Create: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**Steps:**

- [ ] **Step 1: 创建自动配置类**

```java
package com.example.jenkinsdemoformat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertyMapping;

@Configuration
@MapperScan("com.example.jenkinsdemoformat.mapper")
@EnableConfigurationProperties(JenkinsDemoFormatProperties.class)
@PropertyMapping(value = "jenkins.demo-format")
public class JenkinsDemoFormatAutoConfiguration {
    // 配置类内容
}
```

- [ ] **Step 2: 创建 spring.factories 文件**

```file: src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.jenkinsdemoformat.JenkinsDemoFormatAutoConfiguration
```

---

## Task 5: 创建 Web MVC 配置

**Files:**
- Create: `src/main/java/com/example/jenkinsdemoformat/WebMvcConfig.java`

**Steps:**

- [ ] **Step 1: 创建 MVC 配置类**

```java
package com.example.jenkinsdemoformat;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final JenkinsDemoFormatProperties properties;
    
    public WebMvcConfig(JenkinsDemoFormatProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源处理 - 前端页面放在 static目录
        registry.addResourceHandler("/jenkins/**")
                .addResourceLocations("classpath:/static/");
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径重定向到 index.html
        registry.addRedirectViewController("/", "/jenkins/index.html");
    }
}
```

---

## Task 6: 创建前端静态资源

**Files:**
- Create: `src/main/resources/static/index.html`
- Create: `src/main/resources/static/css/style.css`
- Create: `src/main/resources/static/js/app.js`

**Steps:**

- [ ] **Step 1: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jenkins 流水线管理</title>
    <link rel="stylesheet" href="/jenkins/css/style.css">
</head>
<body>
    <div id="app">
        <h1>Jenkins 流水线管理</h1>
        <p>欢迎使用 Jenkins 流水线管理系统</p>
    </div>
    <script src="/jenkins/js/app.js"></script>
</body>
</html>
```

- [ ] **Step 2: 创建简单的 CSS 和 JS 文件**

---

## Task 7: 修改主类为 Spring Boot Starter 标准

**Files:**
- Modify: `JenkinsDemoFormatApplication.java`

**Steps:**

- [ ] **Step 1: 修改主类**

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

---

## Task 8: 验证构建

**Steps:**

- [ ] **Step 1: 执行 Maven 构建**

```bash
./mvnw clean package -DskipTests
```

预期: BUILD SUCCESS

- [ ] **Step 2: 验证生成的 JAR 文件结构**

```bash
unzip -l target/jenkins-demo-format-starter-1.0.0.jar | grep -E "(static|META-INF)"
```

预期: 包含 `BOOT-INF/classes/static/` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

---

## 执行方式

**1. Subagent-Driven (recommended)** - 建议使用 subagent-driven-development
**2. Inline Execution** - 使用 executing-plans

**请选择执行方式？**