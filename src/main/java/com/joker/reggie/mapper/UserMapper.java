package com.joker.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joker.reggie.entity.Category;
import com.joker.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
