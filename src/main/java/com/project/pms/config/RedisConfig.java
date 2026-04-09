package com.project.pms.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @className: RedisConfig
 * @description:  Redis配置类
 * @author: loser
 * @createTime: 2026/1/31 21:05
 */
@Configuration
public class RedisConfig<V> {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
//    @Value("${spring.redis.host}")
//    private String redisHost;
//
//    @Value("${spring.redis.port}")
//    private Integer redisPort;
//
//    @Value("${spring.redis.timeout}")
//    private int timeout;
//
//    @Value("${spring.redis.jedis.pool.max-idle}")
//    private int maxIdle;
//
//    @Value("${spring.redis.jedis.pool.max-wait}")
//    private long maxWaitMillis;
//
//    /**
//     * 创建并配置一个 Jedis 连接池
//     * @return
//     */
//    @Bean
//    @SuppressWarnings("all")
//    public JedisPool redisPoolFactory() {
//        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
//        jedisPoolConfig.setMaxIdle(maxIdle);
//        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
//        return new JedisPool(jedisPoolConfig, redisHost, redisPort, timeout);
//    }
//
//
//    @Bean(name = "redissonClient", destroyMethod = "shutdown")
//    public RedissonClient redissonClient() {
//        try {
//            Config config = new Config();
//            config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
//            RedissonClient redissonClient = Redisson.create(config);
//            return redissonClient;
//        } catch (Exception e) {
//            logger.info("redis配置错误，请检查redis配置");
//        }
//        return null;
//    }

    @Bean("redisTemplate")
    public RedisTemplate<String, V> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, V> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 设置key的序列化方式
        template.setKeySerializer(RedisSerializer.string());
        // 设置Hash key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        // 设置Hash value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }

}
