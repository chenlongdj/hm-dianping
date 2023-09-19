package com.hmdp.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁持有的超时时间，过期后锁过期
     * @return ture代表获取成功，false代表获取失败
     */
    boolean tryLock(long   timeoutSec);

    /**
     * 释放锁
     */
    void unlock();

}
