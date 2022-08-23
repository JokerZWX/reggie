package com.joker.reggie.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    // 开始时间戳 2022年一月一日0时0分0秒
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    // 序列号移动位数
    private static final int MOVE_BITS = 32;

    // 这里也可以使用注解注入
    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 实现全局唯一id的值：采用long类型存储（符号位 + 31位时间戳 + 32位的序列号）
     * @param prefixKey
     * @return
     */
    public long nextId(String prefixKey){
        // 1、生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 2、生成序列号   increment:redis的自增序列
        // 这里为了避免超过序列号的增长范围（序列号采用32位存储，所以最高范围是2的32次方）和 redis单个数值的增长的范围（即2的64次方）
        // 会在序列号后添加一个时间日期
        // 2.1 获取当前日期，这里采用精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("inc:" + prefixKey + ":" + date);
        // 3、拼接并返回
        // 将时间戳的结果往左移32位，即给序列号腾出位置。 序列号的32位采用或运算
        return timestamp << MOVE_BITS | count;
    }


/*    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2022,1,1,0,0,0);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second = " + second);
    }*/
}
