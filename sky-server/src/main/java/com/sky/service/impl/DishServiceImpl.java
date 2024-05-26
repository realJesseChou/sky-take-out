package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.event.ListDataEvent;
import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    /**
     * 添加菜品和对应的口味
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        // dish类接收菜品的属性，并从DishDTO中获取对应的属性
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 插入菜品
        dishMapper.insert(dish);
        // 获取insert语句生成的主键值，这里需要在xml文件中设置对应属性
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 设置口味的菜品id
        flavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(dishId);
        });
        if(flavors != null && flavors.size() > 0){
            // 插入n条菜品口味
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 启用pagehelper
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> pages = dishMapper.pageQuery(dishPageQueryDTO);
        // 封装分页结果返回
        return new PageResult(pages.getTotal(), pages.getResult());
    }

    /**
     * 删除菜品
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 判断菜品是否有在销售中
        for(Long id: ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断菜品是否有在套餐中
        List<Long> setmealDishIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealDishIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 可以删除的话则删除菜品关联的口味数据
        // 删除菜品
//        for (Long id : ids) {
//            dishFlavorMapper.deleteByDishId(id);
//            dishMapper.delete(id);
//        }

        // todo: 优化删除方式
        dishFlavorMapper.deleteBatchByDishIds(ids);
        dishMapper.deleteBatch(ids);

    }

    /**
     * 根据id查询菜品详情
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 1. 根据菜品id查询菜品
        Dish dish = dishMapper.getById(id);
        // 2.根据菜品的id查询菜品口味
        List<DishFlavor> dishFlavor = dishFlavorMapper.getByDishId(id);
        // 3.将菜品口味封装到菜品返回结果对象中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavor);
        // 4. 返回
        return dishVO;
    }

    /**
     * 更新菜品
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        // 1.准备数据dish和dishFlavor
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 1.1 获取菜品口味列表
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 1.2 设置菜品口味的dish_id
        flavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(dish.getId());
        });
        // 1.3 删除当前id菜品的所有口味
        dishFlavorMapper.deleteByDishId(dish.getId());
        // 1.4 插入新的口味
        if(flavors != null && !flavors.isEmpty()){
            dishFlavorMapper.insertBatch(flavors);
        }
        // 2.更新dish表
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        // 1.根据分类id查询菜品
        return dishMapper.getByCategoryId(categoryId);
    }

    /**
     * 设置菜品起停售
     * @param status
     * @param id
     */
    @Override
    public void setOnSale(Integer status, Long id) {
        // 1.设置菜品的状态
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.setOnSale(dish);
    }

}
