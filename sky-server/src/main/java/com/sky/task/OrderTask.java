package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ? ")
//    @Scheduled(cron = "0/5 * * * * ? ")
    public void processTimeoutOrder(){
        log.info("处理超时订单:{}", LocalDateTime.now());
        // 查询超时订单是否存在
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        // 处理超时订单
        if(orders != null && !orders.isEmpty()){
            orders.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }
    }

    /**
     * 处理已完成配送但是未收货订单
     */
    @Scheduled(cron = "0 0 1 * * ? ")
//    @Scheduled(cron = "0/5 * * * * ? ")
    public void processDeliveryOrder(){
        log.info("处理已完成配送但是未收货订单:{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().minusMinutes(60);
        // 查询已完成配送但是未收货订单是否存在
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        // 处理一直处于配送中订单
        if(orders != null && !orders.isEmpty()){
            orders.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("超时收货，自动取消订单");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            });
        }
    }
}
