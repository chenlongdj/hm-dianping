package com.hmdp.utils;


import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

//@PropertySource("/jwt.properties")
//@Component
public class JwtUtils {
    @Value("${singKey}")
    private static String signKey;

    @Value("${expire}")
    private static Long expire;

    public static String generateJwt(Map<String, Object> claims){
        String jwt = Jwts.builder()
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256, signKey)
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .compact();
        return jwt;
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public static Claims parseJWT(String jwt){
        Claims claims = Jwts.parser()
                .setSigningKey(signKey)
                .parseClaimsJws(jwt)
                .getBody();
        return claims;
    }

    /**
     * 判断token
     * @param token
     * @return
     */
    public static  Boolean checkToken(String token){
        if (StrUtil.isBlank(token)){
            return false;
        }
        try {
            Map<String, Object> claims = JwtUtils.parseJWT(token);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
