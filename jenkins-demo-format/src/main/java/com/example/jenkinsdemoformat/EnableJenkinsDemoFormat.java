package com.example.jenkinsdemoformat;

import org.springframework.context.annotation.Import;
import com.example.jenkinsdemoformat.JenkinsDemoFormatAutoConfiguration;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(JenkinsDemoFormatAutoConfiguration.class)
public @interface EnableJenkinsDemoFormat {
}