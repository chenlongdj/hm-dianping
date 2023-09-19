package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @auther CL    自定义锁
 * @date 2023/9/16 0016  13:43
 *
 */

public class SimpleRedisLock implements ILock {
    private StringRedisTemplate stringRedisTemplate;
    private  String name;
    private static final  String KEY_PREFIX="lock:"; //锁前缀
    private static  final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";//uuid 区分不同jvm  线程id来分别不同线程，来避免锁删除错误问题
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }



    //通过lua脚本确保锁删除保持原子性
    //查看继承关系 ctrl+H
    private  static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //1.获取线程标识,
       // long threadId = Thread.currentThread().getId();
        //2.获取线程标识,区别不同进程下线程id冲突
       String threadId =ID_PREFIX +Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);

//自动拆箱装箱问题，避免返回空指针异常 return success;
        return Boolean.TRUE.equals(success);
    }


    @Override
    public void unlock() {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(KEY_PREFIX + name),ID_PREFIX +Thread.currentThread().getId());

    }

//    @Override
//    public void unlock() {
//        //获取线程标识上个线程(锁标识)
//        String threadId =ID_PREFIX +Thread.currentThread().getId();
//        //查询的是目前线程的锁标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        //判断是否一致
//        if (threadId.equals(id)){
//            //释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//
//    }
}
