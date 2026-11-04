// 校园墙项目
// 校园墙后端启动类，Spring Boot 的入口，启动后加载所有控制器、服务和数据库配置。
package com.campuswall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusWallApplication {
// 程序入口：启动 Spring Boot 应用。
    public static void main(String[] args) {
        SpringApplication.run(CampusWallApplication.class, args);
    }
}
