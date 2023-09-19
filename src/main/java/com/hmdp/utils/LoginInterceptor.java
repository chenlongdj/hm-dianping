package com.hmdp.utils;


import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * @auther CL
 * @date 2023/8/26 0026  9:45
 */

public class LoginInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //1.判断是否 需要拦截（ThreadLocal中是否有用户）；
        if (UserHolder.getUser() ==null){
            //不存在，拦截
            response.setStatus(401);
            return false;
        }
        return true;
    }



}
