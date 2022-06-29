package com.joker.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.User;
import com.joker.reggie.service.UserService;
import com.joker.reggie.utils.EMSUtils;
import com.joker.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender javaMailSender;

    /**
     * 获取验证码
     * @param request
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(HttpServletRequest request, @RequestBody User user){
        // 1、获取手机号
        String phone = user.getPhone();
        // 2、判断手机号是否为空
        if (StringUtils.isBlank(phone)){
            return R.error("手机号不能为空");
        }
        // 3、生成随机验证码
        String code = ValidateCodeUtils.generateValidateCode4String(4);
        log.info("发送的验证码为：{}",code);
        // 4、将验证码放入session中
        request.getSession().setAttribute("code",code);
        // 5、通过springboot自带的mail使用邮件发送给用户
        SimpleMailMessage simpleMailMessage = EMSUtils.sendSimpleEmail(code);
        javaMailSender.send(simpleMailMessage);
        return R.success("发送成功！");
    }

    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        // 1、获取当前输入手机号和验证码
        String phone = (String) map.get("phone");
        String code = (String) map.get("code");

        // TODO 2、校验手机号

        // 3、获取session中生成的验证码
        String sessionCode = (String) session.getAttribute("code");
        // TODO 校验验证码 验证码要与手机号相对应，不能出现别人的验证码自己也可以使用
        // 3、进行验证码比对
        if (code == null || !code.equals(sessionCode)){
            return R.error("验证码输入错误，请重试！");
        }
        // 4、判断是否是新用户
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null){
            // 4.1、是新用户，将其信息保存到数据库
            user = new User();
            user.setPhone(phone);
            userService.save(user);
        }
        // 5、登录成功，将userId存入session中
        session.setAttribute("user",user.getId());
        return R.success(user);
    }

    @PostMapping("/logout")
    public R<String> logout(HttpSession session){
        session.removeAttribute("user");

        return R.success("退出成功！");
    }
}
