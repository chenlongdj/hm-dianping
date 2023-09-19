package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result shopTypeList() {
        //1.从redis中查询商铺类型缓存
        List<String> shopTypeJsons = stringRedisTemplate.opsForList().range("cache:shop:type:key", 0, -1);
        //2.判断缓存是否命中
        if (!CollectionUtil.isEmpty(shopTypeJsons)){
            //集合不为空即为命中
            //使用stream流将json集合转为bean集合
            List<ShopType> shopTypeList = shopTypeJsons.stream()
                    .map(item -> JSONUtil.toBean(item, ShopType.class))
                    .sorted(Comparator.comparingInt(ShopType::getSort))
                    .collect(Collectors.toList());
            //返回缓存数据
            return Result.ok(shopTypeList);
        }
        //3.未命中缓存,从数据库查询
        List<ShopType>  typeList = query().list();

        //判断数据库中是否有数据
        if (CollectionUtil.isEmpty(typeList)){
            //不存在则返回一个空集合，避免缓存穿透
            stringRedisTemplate.opsForValue().set("cache:shop:type:key", Collections.emptyList().toString(),2L,TimeUnit.MINUTES);
            return Result.fail("商铺分类信息为空");
        }
        //4.数据库命中
        List<String> shopTypeCache = typeList.stream().map(JSONUtil::toJsonStr).collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll("cache:shop:type:key",shopTypeCache);

        stringRedisTemplate.expire("cache:shop:type:key",30, TimeUnit.MINUTES);


        return Result.ok(typeList);
    }
}
