package com.sky.mapper;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单详情
     * @param orderDetials
     */
    void insertBatch(List<OrderDetail> orderDetails);


    /**
     * 根据订单查询订单详情
     * @param order
     */
    List<OrderDetail> getByOrder(Orders order);
}
