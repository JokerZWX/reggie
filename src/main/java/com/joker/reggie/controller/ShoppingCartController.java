package com.joker.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joker.reggie.common.BaseContext;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.ShoppingCart;
import com.joker.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加菜品或者套餐到购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    @Transactional
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        // 1、获取当前用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        // 2、声明条件
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId,userId);
        // 3、判断添加是套餐还是菜品
        Long dishId = shoppingCart.getDishId();
        if (dishId == null){
            //  3.1、说明添加的是套餐
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }else{
            // 3.2、说明添加的是菜品
            // 这里为了满足同样一份菜，选择了不同口味，所以还需要加上口味的判断
            if (shoppingCart.getDishFlavor() == null){
                // 为空，说明是在菜单中添加
                lambdaQueryWrapper.eq(ShoppingCart::getDishId,dishId);
            }else{
                // 不为空，说明在购物车中进行添加
                lambdaQueryWrapper.eq(ShoppingCart::getDishId,dishId)
                        .eq(ShoppingCart::getDishFlavor,shoppingCart.getDishFlavor());
            }
        }
        // 4、查看当前菜品或者套餐是否在购物车中
        // TODO 暂时无法解决在菜单中多个口味的添加，原因是接收不了口味的信息，导致根据userId和DishId查询会查出大于2的数据出来
        ShoppingCart cart = shoppingCartService.getOne(lambdaQueryWrapper);
        if (cart == null){
            // 4.1、不在购物车中，则添加到购物车中，数量默认为1，并加上时间
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
        } else{
            // 4.2、已经在购物车，再次添加，则数量加1
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartService.updateById(cart);
        }
        return R.success(cart);
    }

    /**
     * 减少菜品或者减少套餐,且数量为0时，移除购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    @Transactional
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){
        // 1、获取当前用户id
        Long userId = BaseContext.getCurrentId();
        // 2、声明条件
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId,userId);
        // 3、判断添加是套餐还是菜品
        Long dishId = shoppingCart.getDishId();
        if (dishId == null){
            //  3.1、说明添加的是套餐
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }else{
            // 3.2、说明添加的是菜品
            // 这里为了满足同样一份菜，选择了不同口味，所以还需要加上口味的判断
            if (shoppingCart.getDishFlavor() == null){
                // 为空，说明是在菜单中删减
                lambdaQueryWrapper.eq(ShoppingCart::getDishId,dishId);
            }else{
                // 不为空，说明在购物车中进行删减
                lambdaQueryWrapper.eq(ShoppingCart::getDishId,dishId)
                        .eq(ShoppingCart::getDishFlavor,shoppingCart.getDishFlavor());
            }
        }
        // 4、查看当前菜品或者套餐
        // TODO 暂时无法解决在菜单中多个口味的删除，原因是接收不了口味的信息，导致根据userId和DishId查询会查出大于2的数据出来
        ShoppingCart cart = shoppingCartService.getOne(lambdaQueryWrapper);
        if (cart == null){
            return R.error("购物车中没有该菜品或者套餐");
        }
        // 5、当前菜品或者套餐数量减1并保存到数据库中
        Integer number = cart.getNumber();
        if (number == 0){
            // 当数量是0，直接在数据库中删除
            shoppingCartService.removeById(cart.getId());
        }
        cart.setNumber(number - 1);
        shoppingCartService.updateById(cart);
        return R.success(cart);
    }

    /**
     * 显示购物车信息
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        // 1、得到当前登录userId
        Long userId = BaseContext.getCurrentId();
        // 2、根据当前userId得到其所有订单信息
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(
                new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId)
                        // 数量小于等于0的移除购物车
                        .gt(ShoppingCart::getNumber,0)
        );
        if (shoppingCarts == null){
            log.info("用户" + userId + "当前购物车没有任何订单！");
        }
        // 3、返回给前端
        return R.success(shoppingCarts);
    }

    /**
     * 清空购物车信息
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean(){
        // 1、获取当前登录userId
        Long userId = BaseContext.getCurrentId();
        boolean remove = shoppingCartService.remove(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));
        if (!remove){
            R.error("清空失败！");
        }
        return R.success("清空成功！");
    }
}
