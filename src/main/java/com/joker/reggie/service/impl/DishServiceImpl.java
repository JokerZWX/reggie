package com.joker.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.reggie.dto.DishDto;
import com.joker.reggie.entity.Dish;
import com.joker.reggie.entity.DishFlavor;
import com.joker.reggie.mapper.DishMapper;
import com.joker.reggie.service.DishFlavorService;
import com.joker.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时插入菜品对应的口味数据
     * @param dishDto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        // 1、保存菜品的基本信息到dish菜品表
        this.save(dishDto);

        // 2、获取菜品id
        Long dishId = dishDto.getId();

        List<DishFlavor> flavors = dishDto.getFlavors();
        // 2、将菜品id依次保存到口味表中
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishId);
        }
        // 新特性写法使用stream流
//        flavors = flavors.stream().map(item -> {
//            item.setDishId(dishId);
//            return item;
//        }).collect(Collectors.toList());

        // 3、保存菜品口味数据到dish_flavor菜品口味表
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public DishDto getByIdWithFlavor(Long id) {
        // 1、创建DishDto对象
        DishDto dishDto = new DishDto();
        // 2、根据菜品id查询对应菜品
        Dish dish = this.getById(id);
        // 3、拷贝给dishDto
        BeanUtils.copyProperties(dish,dishDto);
        // 4、根据id查询对应的菜品口味
        List<DishFlavor> flavors = dishFlavorService.list(
                new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dish.getId()));
        if (flavors != null){
            dishDto.setFlavors(flavors);
        }
        return dishDto;
    }

    @Override
    @Transactional
    public void updateByIdWithFlavor(DishDto dishDto) {
        // 1、修改dish中的内容
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDto,dish);
        this.updateById(dish);
        // 2、先删除dishDto中的口味数据
        dishFlavorService.remove(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId,dish.getId()));
        // 3、再将修改后的口味数据添加到DishFlavor中
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dish.getId());
        }
        dishFlavorService.saveBatch(flavors);
    }
}
