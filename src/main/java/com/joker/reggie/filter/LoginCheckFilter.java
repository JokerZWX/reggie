package com.joker.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.joker.reggie.common.BaseContext;
import com.joker.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
// 设置过滤器的名称和需要过滤的url请求
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {

    //路径匹配器，支持通配符匹配路径
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 向下转型为HttpServletRequest
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 1、获取本次请求的uri
        String uri = request.getRequestURI();

        log.info("拦截到请求 {}",uri);

        String[] urls = new String[]{
          "/employee/login",
          "/employee/logout",
          "/backend/**",
          "/front/**",
          "/common/**",
            //下面是移动端请求
          "/user/sendMsg",
          "/user/login",
        };
        // 2、判断本次请求是否需要处理
        boolean check = check(urls, uri);
        // 3、如果不需要处理，则直接放行
        if (check){
            log.info("本次请求{}不需要处理",uri);
            filterChain.doFilter(request,response);
            return;
        }

        // 4.1、需要处理，判断当前后台员工的登录状态，如果已登录则直接放行
        Long empId = (Long) request.getSession().getAttribute("employee");
        if (empId != null) {
            log.info("用户已登录，用户id为：{}", empId);

//            long id = Thread.currentThread().getId();
//            log.info("线程id为：{}",id);
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request,response);
            return;
        }

        // 4.2、需要处理，判断当前的前台用户登录状态，如果已登录则直接放行
        Long userId = (Long) request.getSession().getAttribute("user");
        if (userId != null) {
            log.info("用户已登录，用户id为：{}", userId);
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }

        log.info("用户未登录");
        // 5、如果未登录则返回未登录结果，通过流的方式向客户端页面响应数据（因为在前端JS里已经写过了跳转到登录页面，这里不需要后端写）
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param requestURI
     * @param urls
     * @return
     */
    public boolean check(String[] urls, String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match){
                return true;
            }
        }
        return false;
    }
}
