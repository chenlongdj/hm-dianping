package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @auther CL
 * @date 2023/9/2 0002  16:09
 */

//@Component
@Slf4j
public class CacheClient {
    //final修饰的成员变量，需要用构造方法注入
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    //超时剔除，将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
    public void set(String key, Object date, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(date),time,unit);
    }
    public void setWithLogicalExpire(String key,Object value,Long time, TimeUnit unit){

        //设置逻辑过期
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    //方法2∶将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
    //方法3∶根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题方法4∶根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
}
