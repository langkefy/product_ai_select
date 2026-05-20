package com.select.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.select.product.mapper")
@EnableScheduling
public class ProductSelectApplication {
    public static void main(String[] args) {
        // 为 1688 SDK (HttpURLConnection) 设置全局超时，避免网络不通时无限阻塞
        System.setProperty("sun.net.client.defaultConnectTimeout", "8000");  // 连接超时 8s
        System.setProperty("sun.net.client.defaultReadTimeout",    "20000"); // 读取超时 20s
        SpringApplication.run(ProductSelectApplication.class, args);
    }
}

