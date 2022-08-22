package com.joker.reggie.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.User;
import com.joker.reggie.service.UserService;
import com.joker.reggie.utils.EMSUtils;
import com.joker.reggie.utils.RegexUtils;
import com.joker.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.joker.reggie.utils.RedisConstants.*;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取验证码
     * @param user
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user){
        // 1、获取手机号
        String phone = user.getPhone();
        // 2、校验手机号是否规范
        if (RegexUtils.isPhoneInvalid(phone)){
            return R.error("手机号格式不正确");
        }
        // 3、生成随机6位数字验证码
        String code = ValidateCodeUtils.generateValidateCode(6).toString();
        log.info("发送的验证码为：{}",code);
        // 4、将验证码放入redis中，设置有效日期5分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code, CACHE_CODE_TTL ,TimeUnit.MINUTES);
        // 5、通过springboot自带的mail使用邮件发送给用户
        SimpleMailMessage simpleMailMessage = EMSUtils.sendSimpleEmail(code);
        javaMailSender.send(simpleMailMessage);
        return R.success("发送成功！");
    }

    /**
     * 用户登录（token版）
     * @param session
     * @param map
     * @return
     */
    /*@PostMapping("/login")
    public R<String> login(HttpSession session, @RequestBody Map map){
        // 1、获取当前输入手机号和验证码
        String phone = (String) map.get("phone");
        String code = (String) map.get("code");

        // 2、校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            return R.error("手机格式不正确");
        }
        // 3、获取redis中生成的验证码
        String codeKey = LOGIN_CODE_KEY + phone;
        String CacheCode = stringRedisTemplate.opsForValue().get(codeKey);
        // 3、进行验证码比对 验证码要与手机号相对应，不能出现别人的验证码自己也可以使用
        if (CacheCode == null || !CacheCode.equals(code)){
            // 不一致，返回提示信息
            return R.error("验证码不一致！");
        }
        // 4、判断是否是新用户
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null){
            // 4.1、是新用户，将其信息保存到数据库
            user = createUserWithPhone(phone);
        }
        // 5、保存用户信息到redis中
        // 5.1、生成随机token，作为登录令牌
        String token = UUID.randomUUID().toString(true);

        // 5.2、将User对象转换为UserDto，保证数据安全
        UserDto userDto = BeanUtil.copyProperties(user, UserDto.class);
        // 5.3、使用redis的hash进行存储（这里只储存id）
        String tokenKey = LOGIN_TOKEN_KEY + token;
        stringRedisTemplate.opsForHash().put(tokenKey, "id", userDto.getId().toString());
        // 7、登录成功，将验证码从redis中删除，将userId存入redis中,并设置有效期30分钟
        stringRedisTemplate.delete(codeKey);
        // 7.1 设置token有效期。按照session默认保存时长（30分钟）保存，如果用户在三十分钟内再接着访问，保存时长重新加载
        stringRedisTemplate.expire(tokenKey,LOGIN_TOKEN_TTL,TimeUnit.MINUTES);
        // 8、返回userDto对象
        return R.success(token);
    }*/


    /**
     * 用户登录（无token版）
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(HttpSession session, @RequestBody Map map){
        // 1、获取当前输入手机号和验证码
        String phone = (String) map.get("phone");
        String code = (String) map.get("code");

        // 2、校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            return R.error("手机格式不正确");
        }
        // 将phone存入session中，便于过滤器取出时
        session.setAttribute("phone", phone);
        // 3、获取redis中生成的验证码
        String codeKey = LOGIN_CODE_KEY + phone;
        String CacheCode = stringRedisTemplate.opsForValue().get(codeKey);
        // 3、进行验证码比对 验证码要与手机号相对应，不能出现别人的验证码自己也可以使用
        if (CacheCode == null || !CacheCode.equals(code)){
            // 不一致，返回提示信息
            return R.error("验证码不一致！");
        }
        // 4、判断是否是新用户
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null){
            // 4.1、是新用户，将其信息保存到数据库
            createUserWithPhone(phone);
        }
        // 5、登录成功，将验证码从redis中删除，将userId存入redis中,并设置有效期30分钟
//        session.setAttribute("user",user.getId());
        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.opsForValue().set(LOGIN_USER_KEY + phone,user.getId().toString(),CACHE_USER_TTL,TimeUnit.MINUTES);
        return R.success(user);
    }

    @PostMapping("/logout")
    public R<String> logout(HttpSession session){
        session.removeAttribute("user");

        return R.success("退出成功！");
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        user.setPhone(phone);
        userService.save(user);
        return user;
    }
}
