package com.hmdp.utils;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @auther CL   redisson创建锁
 * @date 2023/9/17 0017  22:19
 */


@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        //配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.31.137:6379").setPassword("12345");
        //创建RedissonClient
        return Redisson.create(config);

    }
}
