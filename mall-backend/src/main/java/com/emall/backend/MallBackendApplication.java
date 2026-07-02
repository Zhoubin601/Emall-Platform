package com.emall.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
// ✨ 这一行非常重要！它告诉 Spring 去哪里找你的 Mapper 接口
@MapperScan("com.emall.backend.mapper")
@EnableCaching
public class MallBackendApplication {

    public static void main(String[] args) {
        // 这一行代码会点火启动整个 Spring 容器
        SpringApplication.run(MallBackendApplication.class, args);
    }
}