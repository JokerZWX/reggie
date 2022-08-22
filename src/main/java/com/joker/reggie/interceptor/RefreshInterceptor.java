package com.joker.reggie.interceptor;

import cn.hutool.core.util.StrUtil;
import com.joker.reggie.common.BaseContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

import static com.joker.reggie.utils.RedisConstants.LOGIN_TOKEN_KEY;
import static com.joker.reggie.utils.RedisConstants.LOGIN_TOKEN_TTL;

public class RefreshInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    /**
     * TODO token版登录，由于前端目前不太会，token没有保存导致报错401
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1、获取请求头的token
        String token = request.getHeader("authorization");

        // 判断token是否为空，为空就放行，让未登录用户浏览能操作的页面
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 2、基于token查询redis保存的用户
        String tokenKey = LOGIN_TOKEN_KEY + token;
        Long userId = (Long) stringRedisTemplate.opsForHash().get(tokenKey, "id");
        // 3、判断用户是否存在 为空放行  给其查看未登录能操作的页面
        if (userId == null) {
            return true;
        }
        // 4、存在则保存用户信息到 ThreadLocal
        BaseContext.setCurrentId(userId);
        // 6、刷新token的有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        // 7、放行
        return true;
    }
}
