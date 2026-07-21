package com.emall.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String uploadLocation;

    public WebConfig(@Value("${storage.upload-dir:/uploads}") String uploadDir) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        this.uploadLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(86400);
    }
}
