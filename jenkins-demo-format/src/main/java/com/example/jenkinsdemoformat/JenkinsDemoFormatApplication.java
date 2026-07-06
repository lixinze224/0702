package com.example.jenkinsdemoformat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.jenkinsdemoformat.mapper")
public class JenkinsDemoFormatApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(JenkinsDemoFormatApplication.class);
        app.setAdditionalProfiles("standalone");
        app.run(args);
        System.out.println("流水线代理工程启动成功 ");
    }

}