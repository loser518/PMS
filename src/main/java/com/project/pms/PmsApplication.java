package com.project.pms;

import com.project.pms.im.IMServer;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @className: UserInfoVO
 * @description: 项目启动 类
 * @author: loser
 * @createTime: 2026/1/31 22：00
 */
//@MapperScan("com.project.pms.mapper")
@SpringBootApplication
public class PmsApplication {
    public static void main(String[] args) {

        SpringApplication.run(PmsApplication.class, args);
    }

}
