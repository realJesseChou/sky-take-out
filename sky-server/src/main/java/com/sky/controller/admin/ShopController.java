package com.sky.controller.admin;

import com.sky.config.RedisConfiguration;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@Slf4j
@RequestMapping("/admin/shop")
@Api(tags = "商铺管理")
class ShopController {

    private static final String KEY = "SHOP_STATUS";
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取商铺状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("商家获取商铺状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("当前商铺状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

    /**
     * 设置商铺状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("商家设置商铺状态")
    public Result setStatus(@PathVariable Integer status){
        if(status != 0 && status != 1){
            return Result.error("状态值不合法");
        }
        redisTemplate.opsForValue().set(KEY, status);
        log.info("设置商铺状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success();
    }
}
