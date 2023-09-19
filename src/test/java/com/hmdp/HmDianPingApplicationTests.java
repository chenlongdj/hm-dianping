package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private RedisIdWorker redisIdWorker;
    //创建指定数目的线程池
    private ExecutorService ex= Executors.newFixedThreadPool(500);

    @Test
    void redisIdTest() throws InterruptedException {
        CountDownLatch latch=new CountDownLatch(300);
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<100;i++ ){
                    Long id = redisIdWorker.nextId("order");
                    System.out.println("id="+id );
                }
                latch.countDown();
            }
        };
        long begin=System.currentTimeMillis();
        for (int i=0;i<300;i++){
            //提交任务
            ex.submit(runnable);
        }
        latch.await();
        long end=System.currentTimeMillis();
        System.out.println("time="+(end-begin));
    }

}
