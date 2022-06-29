package com.joker.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("插入操作执行时需要进行的操作。。。");
        metaObject.setValue("createTime",LocalDateTime.now());
        metaObject.setValue("updateTime",LocalDateTime.now());
//        long id = Thread.currentThread().getId();
//        log.info("线程id为：{}",id);
        // 这里都先暂时写死
        metaObject.setValue("updateUser",BaseContext.getCurrentId());
        metaObject.setValue("createUser",BaseContext.getCurrentId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("修改操作执行时需要进行的操作。。。");
//        long id = Thread.currentThread().getId();
//        log.info("线程id为：{}",id);
        metaObject.setValue("updateTime", LocalDateTime.now());
        // 解决公共字段自动填充时需要用到session域中的empId：使用ThreadLocal
        metaObject.setValue("updateUser", BaseContext.getCurrentId());
    }
}
