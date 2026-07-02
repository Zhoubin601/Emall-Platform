package com.emall.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 告诉 Spring Boot：去容器里的 /uploads/ 文件夹找图片
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/uploads/");
    }
}