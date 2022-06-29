package com.joker.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.reggie.common.CustomException;
import com.joker.reggie.common.R;
import com.joker.reggie.dto.SetmealDto;
import com.joker.reggie.entity.Category;
import com.joker.reggie.entity.Setmeal;
import com.joker.reggie.entity.SetmealDish;
import com.joker.reggie.mapper.SetmealMapper;
import com.joker.reggie.service.CategoryService;
import com.joker.reggie.service.SetmealDishService;
import com.joker.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 实现通过id查询到SetmealDto
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {

        return getSetmealDto(id);
    }

    @Override
    @Transactional
    public R<String> updateByIdWithDish(SetmealDto setmealDto) {
        // 1、创建新的setmeal对象
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto,setmeal);
        // 2、修改setmeal的数据库信息
        boolean isUpdated = setmealService.updateById(setmeal);
        if (!isUpdated){
            R.error("修改失败！");
        }
        // 3、修改套餐中的菜品信息---->跟口味类似，先删除，后添加
        setmealDishService.remove(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId,setmealDto.getId()));
        // 4、获取setmealDto中的所有菜品信息
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        // 将已经保存到数据生成的setmealId赋给套餐菜品表中的setmeal_id
        setmealDishes = setmealDishes.stream().map(item -> {
            item.setSetmealId(setmeal.getId());
            return item;
        }).collect(Collectors.toList());
        //
        // 4、将其保存到setmeal_dish表中
        boolean isSaved = setmealDishService.saveBatch(setmealDishes);
        if (!isSaved){
            return R.error("修改失败！");
        }
        return R.success("修改套餐信息成功！");
    }

    /**
     * 删除套餐，同时需要删除套餐和菜品的关联数据
     * @param ids
     * @return
     */
    @Override
    public R<String> removeWithDish(List<Long> ids) {
        // 1、查询套餐状态，确认是否可以删除
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Setmeal::getId,ids).eq(Setmeal::getStatus,1);
        int count = this.count(lambdaQueryWrapper);
        if (count > 0){
            // 2、无法删除，则抛出异常
            throw new CustomException("套餐正在售卖中，不可删除！");
        }
        // 3、可以删除，先删除套餐表中的数据
        this.removeByIds(ids);
        // 4、再删除关系表中的数据
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);
        return R.success("删除成功！");
    }


    private SetmealDto getSetmealDto(Long id) {
        // 1、创建SetmealDto对象
        SetmealDto setmealDto = new SetmealDto();
        // 2、通过id查询到setmeal
        Setmeal setmeal = this.getById(id);
        // 3、进行数据拷贝
        BeanUtils.copyProperties(setmeal,setmealDto);
        // 4、通过setmeal获取到类别名称
        Category category = categoryService.getById(setmeal.getCategoryId());
        setmealDto.setCategoryName(category.getName());
        // 5、获取套餐对应的所有菜品
        List<SetmealDish> setmealDishes = setmealDishService
                .list(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, id));
        setmealDto.setSetmealDishes(setmealDishes);
        return setmealDto;
    }
}
