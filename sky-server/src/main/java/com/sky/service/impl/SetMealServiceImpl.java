package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetMealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetMealServiceImpl implements SetMealService {
    @Autowired
    private SetMealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 添加套餐
     * @param setmealDTO
     */
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        // 1.将DTO转换为Setmeal和SetmealDish两个实体
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 2.添加Setmeal
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();   // 获取插入后生成的套餐id
        // 3.添加SetmealDish
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> pages = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new  PageResult(pages.getTotal(), pages.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 1.根据id删除setmeal表中的套餐
        setmealMapper.deleteBatch(ids);
        // 2.根据id删除setmeal_dish表中的套餐与菜品的关系
        setmealDishMapper.deleteBatch(ids);
    }

    /**
     * 根据id查询套餐信息
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        // 1. 查询setmeal获取套餐数据
        Setmeal setmeal =  setmealMapper.getById(id);
        // 2.查询category表获得套餐所属分类数据
        Category category = categoryMapper.getByCategoryId(setmeal.getCategoryId());
        // 3.查询setmeal_dish表获得套餐包含的餐品列表
        List<SetmealDish> setmealDishes = setmealDishMapper.getSetmealDishesBySetmealId(id);
        // 4.将数据封装到SetmealVO对象中返回
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setCategoryName(category.getName());
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }


    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @Transactional
    public SetmealVO update(SetmealDTO setmealDTO) {
        // 1.准备setmeal实体，更新套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        // 2.准备setmeal_dish实体，删除原来的对应关系，插入新的对应关系
        List<Long> ids = new ArrayList<>();
        ids.add(setmealDTO.getId());
        setmealDishMapper.deleteBatch(ids);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmeal.getId());
        });
        setmealDishMapper.insertBatch(setmealDishes);

        // 3.准备SetmealVO实体并返回
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmealDTO, setmealVO);
        setmealVO.setUpdateTime(setmeal.getUpdateTime());
        Category category =  categoryMapper.getByCategoryId(setmeal.getCategoryId());
        setmealVO.setCategoryName(category.getName());

        return setmealVO;
    }

    /**
     * 起售/禁售套餐
     * @param status
     * @param id
     */
    @Override
    public void setOnSale(Integer status, Long id) {
        // 准备Setmeal实体
        Setmeal setmeal = setmealMapper.getById(id);
        setmeal.setStatus(status);
        // 更新id对应setmeal的status字段
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
