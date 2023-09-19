package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //1.根据id从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get("cache:shop:"+id);
        //判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //2.缓存不为空，返回对象
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //TODO 判断 命中的是否是 空值看isNotBlank方法的详解，到这还有三种情况null、空字符串、特殊字符，所以用!null
        if (shopJson!=null){
            return Result.fail("店铺信息不存在");
        }
        //3.如果不存在，根据id数据库查询
        Shop shop = getById(id);
        //不存在，返回错误
        if (shop==null){
            //TODO 如果为null，将null存入redis缓存
            stringRedisTemplate.opsForValue().set("cache:shop:"+id,"",2L, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        //4.存入redis
        stringRedisTemplate.opsForValue().set("cache:shop:"+id,JSONUtil.toJsonStr(shop),30, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id==null){
            return Result.fail("商铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete("cache:shop:"+id);
        return Result.ok();
    }
}
