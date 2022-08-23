package com.joker.reggie.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joker.reggie.common.R;
import com.joker.reggie.dto.DishDto;
import com.joker.reggie.entity.Category;
import com.joker.reggie.entity.Dish;
import com.joker.reggie.entity.DishFlavor;
import com.joker.reggie.service.CategoryService;
import com.joker.reggie.service.DishFlavorService;
import com.joker.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.joker.reggie.utils.RedisConstants.*;

@Slf4j
@RestController
@RequestMapping("/dish")
public class DishFlavorController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    public R<String> add(@RequestBody DishDto dishDto){
        log.info("执行新增菜品操作");
        dishService.saveWithFlavor(dishDto);
        return R.success("新增成功！");
    }

    /**
     * 分页显示所有菜品
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        // 1、创建Page对象，声明每页的大小和当前页
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dtoPage = new Page<>(page,pageSize);
        // 2、创建条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加逻辑删除的条件
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime).eq(Dish::getIsDeleted,0);

        // 3、模糊查询的设置
        lambdaQueryWrapper.like(name != null,Dish::getName,name);

        // 4、按上述条件进行查询
        dishService.page(pageInfo,lambdaQueryWrapper);

        // 5、对象拷贝
        BeanUtils.copyProperties(pageInfo,dtoPage,"records");

        // 6、得到Dish集合
        List<Dish> records = pageInfo.getRecords();

        // 7、为DishDto对象依次设置categoryName
        List<DishDto> dishDtoList = records.stream().map(item -> {
            // 7.1、创建一个新的DishDto对象
            DishDto dishDto = new DishDto();
            // 7.2、将item（这里item其实就是dish对象）拷贝给dishDto
            BeanUtils.copyProperties(item, dishDto);
            // 7.3、通过categoryId到category表中查询到categoryName
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null){
                dishDto.setCategoryName(category.getName());
            }
            return dishDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(dishDtoList);

        return R.success(dtoPage);
    }

    /**
     * 根据id查询对应的菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品信息
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        // 修改dishFlavor中的内容
        // 弊端：只能在原来的基础上做修改，不能添加或者删除口味
//        dishFlavorService.updateBatchById(dishDto.getFlavors());
        dishService.updateByIdWithFlavor(dishDto);
        // 修改当前菜品之后，先清除当前菜品对应的redis缓存
        // 获取当前菜品对应的key
        String key = DISH_NAME_PREFIX + dishDto.getCategoryId() + "_" + dishDto.getStatus() + "_" + dishDto.getIsDeleted();
        redisTemplate.delete(key);
        return R.success("修改菜品成功！");
    }

    /**
     * 修改菜品状态
     */
    @PostMapping("/status/{status}")
    @Transactional
    public R<String> status(@PathVariable Integer status,@RequestParam(value = "ids") List<Long> ids){
        // 1、根据id获取相对应菜品信息
        List<Dish> dishList = dishService.listByIds(ids);

        // 2、遍历所有菜品信息
        for (Dish dish : dishList) {
            // 2、获取当前菜品的状态
            Integer dishStatus = dish.getStatus();
            if (dishStatus == 0){
                status = 1;
            } else if (dishStatus == 1){
                status = 0;
            }
            // 2、将修改好的状态信息放入dish中
            dish.setStatus(status);
            dishService.updateById(dish);
        }

        return R.success("菜品状态修改成功！");
    }

    /**
     * 批量删除菜品----》逻辑删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @Transactional // 声明为事务
    public R<String> delete(@RequestParam("ids") List<Long> ids){
        // 1、根据id查询到对应的菜品
        List<Dish> dishList = dishService.listByIds(ids);
        // 2、实现逻辑删除
        for (Dish dish : dishList) {
            // 2.1、判断是否
            Integer status = dish.getStatus();
            if (status == 1){
                return R.error("菜品正在售卖中，不可删除！");
            }
            // 2.2、批量修改

            dish.setIsDeleted(1);
            dishService.updateById(dish);

            // 2.3、再根据菜品id查询相应口味
            List<DishFlavor> flavors = dishFlavorService.list(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dish.getId()));
            for (DishFlavor flavor : flavors) {
                // 逻辑删除
                flavor.setIsDeleted(1);
                dishFlavorService.updateById(flavor);
            }
        }
        return R.success("删除成功！");
    }

    /**
     * 根据菜品类型查询对应的菜品信息
     * 改：在前端展示时新增的内容：还需要展示出每个菜品的选择规格（如口味等信息），且要添加逻辑删除的条件
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        // 1、判断redis缓存中是否有当前菜品类别信息
        String key = DISH_NAME_PREFIX + dish.getCategoryId() + "_" + dish.getStatus() + "_" + dish.getIsDeleted();
        List<DishDto> dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtoList != null && !dishDtoList.isEmpty()){
            // 2、有，直接返回
            return R.success(dishDtoList);
        }
        if (dishDtoList != null){
            // 为空，给出错误提示信息
            return R.error("你要查询的菜品信息不存在");
        }

        // 3、为null，创建一个新的dishDto
        dishDtoList = new ArrayList<>();

        List<Dish> dishList = null;
        // 4、根据菜品名称查询对应的菜品信息
        if (dish.getName() != null){
            dishList = dishService.list(
                    new LambdaQueryWrapper<Dish>().like(Dish::getName, dish.getName())
                            .orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime)
                            // 4.1、添加查询状态为1（在售）的条件
                            .eq(Dish::getStatus,1)
                            // 4.2、添加逻辑删除为0（未删除）条件
                            .eq(Dish::getIsDeleted,0)
            );
            if (dishList.isEmpty()){
                // 如果没有的话，则直接缓存一个空集合，避免一直查询没有的套餐，减少数据库压力
                redisTemplate.opsForValue().set(key,dishList, CACHE_NULL_TTL + RandomUtil.randomLong(3), TimeUnit.MINUTES);
                return null;
            }
        }
        // 5、根据菜品类型查询对应菜品信息
        if (dish.getCategoryId() != null){
            dishList = dishService.list(
                    new LambdaQueryWrapper<Dish>().eq(Dish::getCategoryId, dish.getCategoryId())
                            .orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime)
                            // 5.1、添加查询状态为1（在售）的条件
                            .eq(Dish::getStatus,1)
                            // 5.2、添加逻辑删除为0（未删除）条件
                            .eq(Dish::getIsDeleted,0)
            );
            if (dishList.isEmpty()){
                // 如果没有的话，则直接缓存一个空集合，避免一直查询没有的套餐，减少数据库压力
                redisTemplate.opsForValue().set(key,dishList, CACHE_NULL_TTL + RandomUtil.randomLong(3), TimeUnit.MINUTES);
                return null;
            }
        }
        // 6、将dish转换为dishDto
        for (Dish dish1 : dishList) {
            // 3.1、添加菜品口味
            DishDto dishDto = dishService.getByIdWithFlavor(dish1.getId());
            // 3.2、添加菜品类型名称
            Category category = categoryService.getById(dish1.getCategoryId());
            dishDto.setCategoryName(category.getName());
            dishDtoList.add(dishDto);
        }
        // 7、如果缓存不存在，则将其加入redis缓存中,并设置有效时间为60分钟
        redisTemplate.opsForValue().set(key,dishDtoList,CACHE_DISHDTO_TTL + RandomUtil.randomLong(3), TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }

}
