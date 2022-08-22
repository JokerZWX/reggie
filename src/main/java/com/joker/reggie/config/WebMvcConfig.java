package com.joker.reggie.config;

import com.joker.reggie.common.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Slf4j
@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始进行静态资源映射...");
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
    }

    /**
     * 使用拦截器根据是否登录拦截相关请求
     * @param registry
     */
    /*@Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/employee/login",
                        "/employee/logout",
                        "/backend/**",
                        "/front/**",
                        "/common/**",
                        //下面是移动端请求
                        "/user/sendMsg",
                        "/user/login"
                ).order(1);
        // token刷新拦截器 （优先执行）
        registry.addInterceptor(new RefreshInterceptor(stringRedisTemplate))
                .order(0);
    }*/

    /**
     * 自定义扩展SpringMVC框架的消息转换器
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("加载消息转换器");
        // 1、创建消息转换器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        // 2、设置对象转换器，底层使用Jackson将Java转换为Json
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        // 3、将上面的消息转换器追加到MVC框架的转换器中
        // 这里注意需要设置追加的索引，放在前面Spring在使用的时候才会优先使用自己的消息转换器
        converters.add(0,messageConverter);
    }
}
