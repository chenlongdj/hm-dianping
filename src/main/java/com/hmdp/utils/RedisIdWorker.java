package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @auther CL
 * @date 2023/9/9 0009  14:51
 */

@Component
public class RedisIdWorker {

    /**
     * redis自增  实现全局唯一id
     * @其他唯一id生成策略
     * UUID
     * snowflake算法
     * 数据库自增
     */
    //开始时间戳
    private static final Long BEGIN_TIMESTAMP=1582329600L;
    private  static  final  int COUNT_BITS=32;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    public  Long nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        //1.1当前时间戳
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        //1.2时间戳
        long timestamp=nowSecond-BEGIN_TIMESTAMP;

        /**
         *  date 避免key不变，最后会超值
         */
        //2.生成序列号
        //2.1获取当天的日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.2自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix +":"+ date);
        //3.拼接
        return timestamp<<COUNT_BITS | count;
    }


//    public static void main(String[] args) {
//        LocalDateTime time= LocalDateTime.of(2020, 2, 22, 12, 0, 0);
//        long timeSecond = time.toEpochSecond(ZoneOffset.UTC);
//
//        System.out.println("second="+timeSecond); //  1582329600L
//        System.out.println(LocalDateTime.now());
//    }
}
