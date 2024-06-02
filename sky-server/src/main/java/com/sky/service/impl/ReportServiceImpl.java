package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 构建dataList
        List<LocalDate> dataList = new ArrayList<>();       // 存放每天的日期
        for(LocalDate date = begin; date.isBefore(end.plusDays(1)); date = date.plusDays(1)){
            dataList.add(date);
        }
        // 查询订单表，获取已完成的订单，并且日期再begin与end之间的订单
        List<Double> turnoverList = new ArrayList<>();
        dataList.forEach(date -> {
            // 查询当天的订单
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED); // 已完成的订单

            // 计算当天的营业额
            Double turnover =  orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        });
        // 计算每天的营业额
        // 封装成TurnoverReportVO对象返回

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dataList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 构建dataList
        List<LocalDate> dataList = new ArrayList<>();       // 存放每天的日期
        for (LocalDate date = begin; date.isBefore(end.plusDays(1)); date = date.plusDays(1)) {
            dataList.add(date);
        }

        // 查询用户总量，构建totalUserList
        // 查询新增用户，构建newUserList
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        dataList.forEach(date -> {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 查询历史用户总量
            Map map = new HashMap();
            map.put("end", endTime);

            Integer totalUser = userMapper.countByMap(map);
            totalUser = totalUser == null ? 0 : totalUser;
            totalUserList.add(totalUser);

            // 查询新增用户
            map.put("begin", beginTime);

            Integer newUser = userMapper.countByMap(map);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);
        });

        return UserReportVO.builder()
                .dateList(StringUtils.join(dataList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 构建dataList
        List<LocalDate> dataList = new ArrayList<>();       // 存放每天的日期
        for(LocalDate date = begin; date.isBefore(end.plusDays(1)); date = date.plusDays(1)){
            dataList.add(date);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        dataList.forEach(date -> {
            // 查询每日的订单量

            // 当日订单总数
            Integer totalOrder = countOrder(date, date, null);
            totalOrder = totalOrder == null ? 0 : totalOrder;


            // 当日有效订单数
            Integer validOrder = countOrder(date, date, Orders.COMPLETED);
            validOrder = validOrder == null ? 0 : validOrder;

            orderCountList.add(totalOrder);
            validOrderCountList.add(validOrder);
        });


        // 订单总数
        Integer totalOrderCount = countOrder(null, end, null);
        totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;

        // 有效订单数
        Integer validOrderCount = countOrder(null, end, Orders.COMPLETED);
        validOrderCount = validOrderCount == null ? 0 : validOrderCount;
        // 订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0)    orderCompletionRate = 1.0 * validOrderCount / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dataList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }


    /**
     * 查询订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer countOrder(LocalDate begin, LocalDate end, Integer status){
        LocalDateTime beginTime = begin != null ? LocalDateTime.of(begin, LocalTime.MIN) : null;
        LocalDateTime endTime =  end != null ? LocalDateTime.of(end, LocalTime.MAX) : null;

        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }


    /**
     * 销量前十排行榜
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10Statistics(LocalDate begin, LocalDate end) {
        // 查询销量前十的商品
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(map);
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        goodsSalesDTOList.forEach(goodsSalesDTO -> {
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber());
        });

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

}
