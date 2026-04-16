package com.project.pms.utils;

import com.project.pms.entity.constants.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @className: JwtUtil
 * @description: jwt工具类
 * @author: loser
 * @createTime: 2026/1/31 21:09
 */
@RequiredArgsConstructor
@Component("jwtUtil")
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final RedisUtil redisUtil;

    /**
     * 生成随机uuid
     *
     * @return
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取token密钥
     *
     * @return 加密后的token密钥
     */
    public static SecretKey getTokenSecret() {
        byte[] encodeKey = Base64.getDecoder().decode(Constants.JWT_SECRET_KEY);
        return new SecretKeySpec(encodeKey, 0, encodeKey.length, "HmacSHA256");
    }

    /**
     * 创建token
     *
     * @param uid  用户id
     * @param role 用户角色
     * @return
     */
    public String createToken(String uid, String role) {
        String uuid = getUUID();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        SecretKey secretKey = getTokenSecret();
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + Constants.JWT_TTL * 1000;
        Date expDate = new Date(expMillis);

        String token = Jwts.builder()
                .setId(uuid)    // 随机id，用于生成无规则token
                .setSubject(uid)    // 加密主体
                .claim("role", role)    // token角色参数 user/admin 用于区分普通用户和管理员
                .signWith(signatureAlgorithm, secretKey)
                .setIssuedAt(now)
                .setExpiration(expDate)
                .compact();

        try {
            //缓存token信息，管理员和用户之间不要冲突
            redisUtil.setExValue(Constants.REDIS_KEY_TOKEN + role + ":" + uid, token, Constants.JWT_TTL * 1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("存储redis数据异常", e);
        }
        return token;
    }

    /**
     * 获取Claims信息
     *
     * @param token token
     * @return token的claims
     */
    public static Claims getAllClaimsFromToken(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(getTokenSecret())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException eje) {
            claims = null;
//            log.error("获取token信息异常，jwt已过期");
        } catch (Exception e) {
            claims = null;
//            log.error("获取token信息失败", e);
        }
        return claims;
    }


    /**
     * 获取token主题，即uid
     *
     * @param token token
     * @return uid的字符串类型
     */
    public static String getSubjectFromToken(String token) {
        String subject;
        try {
            Claims claims = getAllClaimsFromToken(token);
            subject = claims.getSubject();
        } catch (Exception e) {
            subject = null;
            log.error("从token里获取不到主题", e);
        }
        return subject;
    }

    /**
     * 在token里获取对应参数的值
     *
     * @param token token
     * @param param 参数名
     * @return 参数值
     */
    public static String getClaimFromToken(String token, String param) {
        Claims claims = getAllClaimsFromToken(token);
        if (null == claims) {
            return "";
        }
        if (claims.containsKey(param)) {
            return claims.get(param).toString();
        }
        return "";
    }

    /**
     * 校验传送来的token和缓存的token是否一致
     *
     * @param token token
     * @return true/false
     */
    public boolean verifyToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (null == claims) {
            return false;
        }
        String uid = claims.getSubject();
        String role;

        if (claims.containsKey("role")) {
            role = claims.get("role").toString();
        } else {
            role = "";
        }
//        log.info("用户角色:{}", role);
        String cacheToken;
        try {
            cacheToken = String.valueOf(redisUtil.getValue(Constants.REDIS_KEY_TOKEN + role + ":" + uid));
        } catch (Exception e) {
            cacheToken = null;
            log.error("获取不到缓存的token", e);
        }
//        log.info("传送来的token:{}", token);
//        log.info("缓存的token:{}", cacheToken);
        return StringUtils.equals(token, cacheToken);
    }
}

