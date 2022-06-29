package com.joker.reggie.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 邮箱发送工具类
 */
public class EMSUtils {

    @Autowired
    private JavaMailSender javaMailSender;

    /**
     * 发送简答的邮箱验证码
     * @param code
     */
    public static SimpleMailMessage sendSimpleEmail(String code){
        // 1、创建简单邮箱对象
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        // 2、设置邮件主题
        simpleMailMessage.setSubject("短信验证码");
        // 3、设置文本内容
        simpleMailMessage.setText("【瑞吉外卖】你的验证码是： " + code + "，请在五分钟内完成验证！");
        // 4、设置写信人的邮箱地址
        simpleMailMessage.setFrom("240133295@qq.com");
        // 5、设置收件人的邮箱地址
        simpleMailMessage.setTo("240133295@qq.com");
        // 6、返回
        return simpleMailMessage;
    }
}
