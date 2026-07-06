package com.example.jenkinsdemoformat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.example.jenkinsdemoformat.config.RestConfig;
import com.example.jenkinsdemoformat.config.WebMvcConfig;
import com.example.jenkinsdemoformat.config.OpenApiConfig;

@Configuration
@MapperScan("com.example.jenkinsdemoformat.mapper")
@ConditionalOnProperty(name = "jenkins.demo.format.enabled", havingValue = "true", matchIfMissing = true)
@Import({RestConfig.class, WebMvcConfig.class, OpenApiConfig.class})
public class JenkinsDemoFormatAutoConfiguration {
}