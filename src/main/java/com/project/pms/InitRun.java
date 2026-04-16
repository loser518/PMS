package com.project.pms;

import com.project.pms.im.IMServer;
import lombok.RequiredArgsConstructor;
import org.redisson.client.RedisConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @className: InitRun
 * @description: 项目启动初始化类
 * @author: loser
 * @createTime: 2026/2/10 15:20
 */
@Component
@RequiredArgsConstructor
public class InitRun implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(InitRun.class);

    private final DataSource dataSource;
    private final IMServer imServer;
    @Override
    public void run(ApplicationArguments args) throws Exception {

        try{
            dataSource.getConnection();
            new Thread(imServer).start();
            logger.info("服务启动成功！");
        } catch (SQLException e) {
            logger.error("数据库配置失败，请检查数据库配置");
        } catch (RedisConnectionException e) {
            logger.error("redis配置失败，请检查redis配置");
        } catch (Exception e){
            logger.error("服务启动失败",e);
        }
    }
}
