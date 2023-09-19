package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.JwtUtils;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     *session发送短信验证码
     */
//    @Override
//    public Result sendCode(String phone, HttpSession session) {
//        //1.校验手机号
//        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
//        //2.如果不符合，返回错误信息
//        if (phoneInvalid){
//            Result.fail("号码不存在");
//        }
//        //3.符合，生成验证码
//
//        String code = RandomUtil.randomNumbers(6);
//        //4.保存验证码到session
//        session.setAttribute("code",code);
//        //发送验证码
//
//        log.debug("发送验证码成功：{}",code);
//
//        return Result.ok();
//    }




    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //2.如果不符合，返回错误信息
        if (phoneInvalid){
            Result.fail("号码不存在");
        }
        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到redis  //set key value ex 120
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY +phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //发送验证码

        log.debug("发送验证码成功：{}",code);

        return Result.ok();
    }




//    @Override
//    public Result login(LoginFormDTO loginForm, HttpSession session) {
//
//       /**
//         * 短信验证码登入和注册功能
//         */
//
//        //1.验证手机号
//        String phone = loginForm.getPhone();
//        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
//              //2.如果不符合，返回错误信息
//        if (phoneInvalid){
//            Result.fail("号码格式不对");
//        }
//        //2.校验验证码
//        String code = loginForm.getCode();
//        Object cacheCode = session.getAttribute("code");
//        if (cacheCode == null || !cacheCode.toString().equals(code)){
//            //3.不一致，报错
//            return Result.fail("验证码错误");
//        }
//        //4.一致，根据手机查询用户
//        User user = query().eq("phone", phone).one();
//
//        //5.判断用户是否存在
//        if (user==null){
//            //6.不存在，创建新用户并保存
//            user=creatUserWithPhone(phone);
//        }
//        //7.保存信息到session
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
//        return Result.ok();
//    }
//
//    private User creatUserWithPhone(String phone) {
//        User user=new User();
//        user.setPhone(phone);
//        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(6));
//        save(user);
//        return user;
//    }



    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        /**
         * redis 共享session短信验证码登入和注册功能
         */

        //1.验证手机号
        String phone = loginForm.getPhone();
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //2.如果不符合，返回错误信息
        if (phoneInvalid){
            Result.fail("号码格式不对");
        }
        //2.校验验证码
        String code = loginForm.getCode();
        String redisCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY +phone);
        if (redisCode == null || !redisCode.equals(code)){
            //3.不一致，报错
            return Result.fail("验证码错误");
        }
        //4.一致，根据手机查询用户

        User user = query().eq("phone", phone).one();

        //5.判断用户是否存在
        if (user==null){
            //6.不存在，创建新用户并保存
            user=creatUserWithPhone(phone);
        }
        //7.保存信息到redis
        //7.1随机生成token，作为登入令牌
        String token = UUID.randomUUID().toString(true);
        //7.2 将User对象转化为HashMap
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));//将实体里的类型转换
        //7.3存储
        String tokenKey="login:token:" + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        //7.4 设置token有效期
        stringRedisTemplate.expire(tokenKey,30,TimeUnit.MINUTES);

        return Result.ok(token);
    }

        private User creatUserWithPhone(String phone) {
            User user=new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(6));
            save(user);
            return user;
        }




}
