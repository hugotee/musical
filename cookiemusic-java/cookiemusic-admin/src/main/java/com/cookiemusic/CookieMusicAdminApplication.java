package com.cookiemusic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.cookiemusic"})
@MapperScan(basePackages = {"com.cookiemusic.mappers"})
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
public class CookieMusicAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(CookieMusicAdminApplication.class, args);
    }
}