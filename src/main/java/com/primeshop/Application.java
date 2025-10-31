package com.primeshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

@SpringBootApplication
@EnableCaching
// @DependsOn("flyway")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // @Bean
    // public FlywayMigrationStrategy flywayMigrationStrategy() {
    //     return flyway -> {
    //         flyway.repair();   // Sửa metadata nếu có lỗi trước đó
    //         flyway.migrate();  // Chạy migration
    //     };
    // }
}