package com.joker.reggie.interceptor;

import com.joker.reggie.common.BaseContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1、判断是否需要拦截
        if (BaseContext.getCurrentId() == null) {
            // 不存在，则需要拦截（未认证）
            response.setStatus(401);
            return false;
        }
        // 有 --》 直接放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户Id，以免信息泄漏
        BaseContext.removeCurrentId();
    }
}
