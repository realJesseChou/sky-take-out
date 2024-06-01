package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/admin/order")
@Api(tags = "管理端订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 管理端订单分页查询
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("管理端订单分页查询")
    public Result<PageResult> search(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("管理端订单分页查询");
        PageResult pageResult = orderService.page(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取各个状态的订单数量
     */
    @GetMapping("/statistics")
    @ApiOperation("获取各个状态的订单数量")
    private Result<OrderStatisticsVO> getOrderStatistics(){
        log.info("获取各个状态的订单数量");
        OrderStatisticsVO orderStatisticsVO = orderService.getOrderStatistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 获取订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("根据订单id获取订单详情")
    public Result<OrderVO> getOrderDetails(@PathVariable Long id){
        log.info("根据订单id获取订单详情");
        OrderVO orderVO = orderService.getDetailByID(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param id
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单");
        orderService.confirmOrder(ordersConfirmDTO);
        return Result.success();
    }


    /**
     * 拒单
     * @param ordersRejectionDTO
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejectOrder(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception{
        log.info("拒单");
        orderService.rejectOrder(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 商家取消订单
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception{
        log.info("取消订单{}", ordersCancelDTO);
        orderService.adminCancelOrder(ordersCancelDTO);
        return null;
    }


    /**
     * 派送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result deliveryOrder(@PathVariable Long id){
        log.info("派送订单");
        orderService.deliveryOrder(id);
        return null;
    }


    /**
     * 完成订单
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result completeOrder(@PathVariable Long id){
        log.info("完成订单");
        orderService.completeOrder(id);
        return null;
    }

}
