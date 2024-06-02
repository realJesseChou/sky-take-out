package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;
    @Autowired
    private WebSocketServer webSocketServer;

    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 1.处理各种异常（地址簿异常/购物车异常）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null) {
            // 地址簿异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> carts = shoppingCartMapper.list(shoppingCart);

        if(carts == null || carts.isEmpty()){
            // 购物车异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 2.向Orders表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));   // 设置订单号
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        AddressBook address = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        orders.setAddress(address.getProvinceName() + address.getCityName() + address.getDistrictName() + address.getDetail());
        orders.setUserName(userMapper.getById(userId).getName());

        orderMapper.insert(orders);

        List<OrderDetail> orderDetails = new ArrayList<>();
        // 3.向order_detail表插入多条数据
        for(ShoppingCart cart : carts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        // 批量插入订单详情
        orderDetailMapper.insertBatch(orderDetails);

        // 4.清空购物车
        shoppingCartMapper.delete(shoppingCart);

        // 5.封装返回结果返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
        paySuccess(ordersPaymentDTO.getOrderNumber());

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));

//        return vo;
        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过websocket通知商家
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId().toString());
        map.put("content", "订单号:" + outTradeNo);

        String jsonString = JSON.toJSONString(map);
        // 推送消息
        webSocketServer.sendToAllClient(jsonString);
    }


    /**
     * 订单分页查询
     * @return
     */
    @Override
    public PageResult page(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 开启分页查询
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 查询当前用户状态为status的订单
        List<Orders> pages = orderMapper.page(ordersPageQueryDTO);

        // 新建返回结果
        List<OrderVO> list = new ArrayList<>();

        // 为每个订单查询详情，并且封装到OrderVO中
        for(Orders order : pages){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);

            // 查询订单详情
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrder(order);
            orderVO.setOrderDetailList(orderDetails);
            orderVO.setOrderDishes(orderDetails.toString());
            list.add(orderVO);
        }
        return new PageResult(list.size(), list);
    }

    /**
     * 根据订单id获取订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO getDetailByID(Long id) {
        // 根据订单id查询order表，获取订单数据
        Orders orders = orderMapper.getById(id);
        // 查询order_detail表获取订单对应的商品详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrder(orders);
        // 组合数据成OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancelOrder(Long id) throws Exception {
        // 根据订单id查询订单
        Orders orders = orderMapper.getById(id);
        // 校验订单是否存在
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 对订单状态进行判断进行判断
        Integer status = orders.getStatus();
        // 不处于待付款和待接单状态，则不能直接退款
        if(status > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 取消订单逻辑
        Orders newOrder = new Orders();
        newOrder.setId(orders.getId());

        // 对于待接单状态的订单，需要进行退款
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            // 调用退款接口
            weChatPayUtil.refund(
                    orders.getNumber(),                 // 商户订单号
                    orders.getNumber(),                 // 商户退款单号
                    BigDecimal.valueOf(orders.getAmount().doubleValue()),   // 退款金额
                    BigDecimal.valueOf(orders.getAmount().doubleValue())    // 原订单金额
            );
            newOrder.setPayStatus(Orders.REFUND);
        }
        // 设置取消订单相关状态
        newOrder.setStatus(Orders.CANCELLED);
        newOrder.setCancelReason("用户取消订单");
        newOrder.setCancelTime(LocalDateTime.now());
        // 更新
        orderMapper.update(newOrder);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void BuyAgain(Long id) {
        // 根据id订单详情
        Orders order = new Orders();
        order.setId(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrder(order);
        // 获取用户userId
        Long userId = BaseContext.getCurrentId();
        // 将订单详情加入购物车
        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);

            return shoppingCart;
        }).collect(Collectors.toList());

        // 插入购物车对象
        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    /**
     * 获取各个状态的订单数量
     */
    @Override
    public OrderStatisticsVO getOrderStatistics() {
        // 查询待接单数量
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        // 查询待派送数量
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        // 查询派送中数量
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        // 封装返回数据并返回
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param id
     */
    @Override
    public void confirmOrder(OrdersConfirmDTO ordersConfirmDTO) {
        // 根据订单id查询订单
        Orders orders = orderMapper.getById(ordersConfirmDTO.getId());
        // 判断订单状态是否为待接单且支付状态为已支付
        if(!orders.getStatus().equals(Orders.TO_BE_CONFIRMED) || !orders.getPayStatus().equals(Orders.PAID)) {
            // 不是则报错
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 是则更新订单状态为已接单
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejectOrder(OrdersRejectionDTO ordersRejectionDTO) throws Exception{
        // 根据订单id查询订单
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        // 判断订单状态是否为待接单
        if(orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 不是则报错
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 是则更新订单状态为已取消，并且为用户退款
        Integer payStatus = orders.getPayStatus();
        if(payStatus.equals(Orders.PAID)){
            // 调用退款接口
//            String refund =  weChatPayUtil.refund(
//                    orders.getNumber(),                 // 商户订单号
//                    orders.getNumber(),                 // 商户退款单号
//                    BigDecimal.valueOf(orders.getAmount().doubleValue()),   // 退款金额
//                    BigDecimal.valueOf(orders.getAmount().doubleValue())    // 原订单金额
//            );
//            log.info("申请退款：{}", refund);
            orders.setPayStatus(Orders.REFUND);
        }
        // 更新订单状态
        Orders newOrder = new Orders();
        newOrder.setId(orders.getId());
        newOrder.setStatus(Orders.CANCELLED);
        newOrder.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        newOrder.setCancelTime(LocalDateTime.now());

        orderMapper.update(newOrder);
    }

    /**
     * 商家取消订单
     * @return
     */
    @Override
    public void adminCancelOrder(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        // 如果订单已经支付，那么需要给用户退款
        Integer payStatus = orders.getPayStatus();
        if(payStatus.equals(Orders.PAID)){
            // 给用户退款
            // 调用退款接口
            String refund =  weChatPayUtil.refund(
                    orders.getNumber(),                 // 商户订单号
                    orders.getNumber(),                 // 商户退款单号
                    BigDecimal.valueOf(orders.getAmount().doubleValue()),   // 退款金额
                    BigDecimal.valueOf(orders.getAmount().doubleValue())    // 原订单金额
            );
            log.info("申请退款：{}", refund);
        }
        // 直接将订单状态设置为已取消
        Orders newOrder = new Orders();
        newOrder.setId(orders.getId());
        newOrder.setStatus(Orders.CANCELLED);
        newOrder.setCancelReason(ordersCancelDTO.getCancelReason());
        newOrder.setCancelTime(LocalDateTime.now());
        newOrder.setPayStatus(Orders.REFUND);

        orderMapper.update(newOrder);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void deliveryOrder(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        // 只有订单状态为已接单的才可以派送
        if(orders == null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 更新订单状态为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void completeOrder(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        // 只有订单状态为派送中的才可以完成订单
        if(orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 更新订单状态为已完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 提醒商家接单
     * @param id
     */
    @Override
    public void remindOrder(Long id) {
        // 查询订单
        Orders orders = orderMapper.getById(id);
        // 校验订单是否存在
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号:" + orders.getNumber());

        String jsonString = JSON.toJSONString(map);
        // 推送消息
        webSocketServer.sendToAllClient(jsonString);
    }
}
