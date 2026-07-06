package com.example.jenkinsdemoformat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "jenkins.demo.format.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jenkins Pipeline Format API")
                        .description("Jenkins 流水线格式校验和编排 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Jenkins Demo Format")
                                .url("https://github.com/jenkins-demo-format")));
    }
}
