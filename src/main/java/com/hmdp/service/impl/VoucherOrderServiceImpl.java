package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.RedissonConfig;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 实现优惠价的秒杀下单
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result flashSaleSeckillVoucher(Long voucherId) {
        //1.查询优惠卷
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        boolean after = seckillVoucher.getBeginTime().isAfter(LocalDateTime.now());
        if (after) {
            return Result.fail("秒杀未开始");
        }
        //3.判断秒杀是否已经结束
        boolean before = seckillVoucher.getEndTime().isBefore(LocalDateTime.now());
        if (before) {
            return Result.fail("秒杀已经结束");
        }
        //4.判断库存是否充足
        Integer stock = seckillVoucher.getStock();
        if (stock < 1) {
            return Result.fail("库存已经不足");
        }
        //通过用户id来作为锁，确保一人一单
        //userId.toString()和userId.toString().intern()的区别
        Long userId = UserHolder.getUser().getId();



        /**
         * redis实现分布式锁，避免同一用户不同进程导致重复下单
         * 1.自定义锁
         */
//        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate,"order:"+userId);

        /**
         * 2.redisson api 创建锁

         */
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();

        //判断是否获取锁
        if (!isLock){
            //获取锁失败，返回错误或重试
            return Result.fail("一个用户只能获取一次锁");
        }
        try {
            //获取代理对象（事务）
            IVoucherOrderService proxy= (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            //释放锁
            lock.unlock();

        }


//单进程下多进程锁秒杀单，
//        synchronized (userId.toString().intern()){
//
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }

    }

    /**
     * 创建订单，一人一单，
     * @param voucherId
     * @return
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //先判断VoucherOrder是否一人一单
        //5.1用户id
        Long userId = UserHolder.getUser().getId();
        //5.2查询订单
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();

        //5.3订单是否存在
        if (count > 0) {
            //5.4订单已经存在
            return Result.fail("用户已经购买过！");
        }

        //6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
//                .eq("stock",seckillVoucher.getStock())
                .update();
//        LambdaUpdateWrapper<SeckillVoucher> updateWrapper=new LambdaUpdateWrapper<>();
//        updateWrapper.setSql("stock=stock-1");
//        updateWrapper.eq(SeckillVoucher::getVoucherId,voucherId);
//        boolean update = seckillVoucherService.update(updateWrapper);

        if (!success) {
            return Result.fail("库存不足");
        }

        //6.1创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.2订单id
        Long id = redisIdWorker.nextId("order");
        voucherOrder.setId(id);
        //6.3用户id

        voucherOrder.setUserId(userId);
        //6.4代金卷id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //7.返回订单id
        return Result.ok(id);
    }

}
