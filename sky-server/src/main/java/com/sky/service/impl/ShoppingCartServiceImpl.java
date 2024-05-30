package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetMealMapper setMealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 判断商品是否已经在购物车内
        ShoppingCart shoppingCart = new ShoppingCart();
        // 拷贝购物车dishId、setmealId、dishFlavor属性，并赋值userId属性
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);


        // 结果判断
        if(list != null && !list.isEmpty()){
            // 如果存在则数量加1
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberNyId(cart);
        }else{
            // 如果不存在则添加购物车记录
            Long dishId = shoppingCartDTO.getDishId();
            Long seatmealId = shoppingCartDTO.getSetmealId();
            // 判断首次添加到购物车的是菜品还是套餐
            if(dishId != null){
                // 如果是菜品，则联合去查找菜品的具体信息
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            }else{
                // 如果是套餐，则联合去查找套餐的具体信息
                Setmeal setmeal = setMealMapper.getById(seatmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            // 结果存入到shopping_cart表中
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查询购物车的物品
     */
    @Override
    public List<ShoppingCart> list() {
        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        // 查询当前用户的购物车记录
        List<ShoppingCart> res =  shoppingCartMapper.list(shoppingCart);
        return res;
    }


    /**
     * 减少购物车中的物品
     * @param shoppingCartDTO
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();
        // 获取要删除的菜品/套餐id和口味数据
        Long dishId = shoppingCartDTO.getDishId();
        Long setmealId = shoppingCartDTO.getSetmealId();
        String dishFlavor = shoppingCartDTO.getDishFlavor();
        // 组合成shoppingcart
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .dishId(dishId)
                .setmealId(setmealId)
                .dishFlavor(dishFlavor)
                .build();
        // 查询购物车中的物品
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        // 判断
        if(list != null && !list.isEmpty()){
            ShoppingCart cart = list.get(0);
            if(cart.getNumber() == 1){
                // 数量为1则删除记录
                shoppingCartMapper.delete(cart);
            } else if (cart.getNumber() > 1) {
                // 数量大于1则数量减1
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.updateNumberNyId(cart);
            }
        }
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        // 获取用户id
        Long userId = BaseContext.getCurrentId();
        // 构建shoppingcart对象
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        // 删除
        shoppingCartMapper.delete(shoppingCart);
    }

}
