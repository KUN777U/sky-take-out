package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //获取当前线程中的用户的id
        Long userId = BaseContext.getCurrentId();
        //根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        //将订单详情对象转换为购物车对象(这段代码就是把历史订单中的某道菜，完美复制并转换成当前用户的购物车商品)
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x->{ //x代表流中的每个OrderDetail对象（遍历变量名，随便起），花括号里的逻辑就是把OrderDetail转换成ShoppingCart对象
            ShoppingCart shoppingCart = new ShoppingCart(); //造一个空的购物车对象，准备用来存储数据
            //将原订单详情里面的菜品信息重新复制到购物车对象中，把OrderDetail和ShoppingCart属性名字一样的复制过来，
            //OrderDetail中的id是他自己在数据库的主键ID，复制过去后往购物车里插入数据是，会触发主键冲突报错
            BeanUtils.copyProperties(x,shoppingCart,"id");//所以这里特意告诉工具：“别复制 ID，让购物车自己生成新的主键！
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList()); //把流里的元素装箱打包成列表
        //将购物车对象批量插入购物车表
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理员各个订单数量统计信息
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //根据状态，分别拆查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress= orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        //将查询出的数据封装到orderStatisticsVO中
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 管理员端接单（商家接单其实就是将订单状态信息修改为“已接单”）
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 管理员查询订单管理列表
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionsearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //开启分页查询
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        //执行查询调用Mapper查数据库，返回Page<Orders>对象
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //部分订单状态，需额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(),orderVOList);
        //查询出来的Orders实体里只有订单号、总价、下单时间这些基础信息，但前端后台管理页面需要订单菜品信息，所以需要加工成OrderVO对象
    }

    //该方法就是将查出来的基础订单，加工成带菜品明细的VO对象
    private List<OrderVO> getOrderVOList(Page<Orders> page){
        //需要返回订单菜品信息，自定义OrderVO响应结果
        ArrayList<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult(); //从PageHelper分页结果中取出当前页的订单数据
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                //将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                //将订单菜品信息转换为字符串
                String orderDishes = getOrderDishesStr(orders);
                //将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }
    private String getOrderDishesStr (Orders orders) {
        //查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        //将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3）
        List<String> orderDishList = orderDetailList.stream().map(x->{
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            //这里用java的流（Stream）循环遍历里每一道菜。把每道菜的名称和数量拼起来并在末尾加上一个分号;作为分隔符
            return orderDish;
        }).collect(Collectors.toList());
        //将该订单对应的所有菜品信息拼接在一起
        return String.join("，",orderDishList);
    }

    /**
     * 用户取消订单
     * @param id
     * @throws Exception
     */

    @Override
    public void userCancelById(Long id) throws Exception {
        Orders ordersDB = orderMapper.getById(id);
        //判断订单是否存在
        if (ordersDB == null) {
            //不存在则抛出异常
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1.待付款 2.待接单 3.已结单 4.派送中 5.已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);

        }
        //以上验证都通过后，此时订单处于待支付和接单状态下
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        //订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态、取消原因、取消时间
        orders.setStatus(orders.CANCELLED);
        //取消原因
        orders.setCancelReason("用户取消订单");
        //取消时间
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 用户查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);

        //查询该订单id对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        //将该订单旧机器详情封装到orderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public PageResult page(int page, int pageSize, Integer status) {
        /**
         * 用户查询历史订单列表,用户端订单分页查询
         * @param page
         * @param pageSize
         * @param status
         * @return
         */

         PageHelper.startPage(page,pageSize); //启动分页
         OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO(); //创建订单分页查询DTO对象
         ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());//从当前线程中获取用户的id，作为查询条件
         ordersPageQueryDTO.setStatus(status);
         // 去数据库根据用户id查询数据
         Page<Orders> page1 = orderMapper.pageQuery(ordersPageQueryDTO);

         List<OrderVO> list = new ArrayList<>();
         //遍历这些数据
         if (page1 != null && page1.getTotal() > 0) {
             for (Orders order : page1) {
                 Long orderId = order.getId(); //拿到订单id
                 // 根据订单id查询订单明细
                 List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
                 OrderVO orderVO = new OrderVO(); //把订单信息和菜品列表拼成一个VO对象
                 //对象属性拷贝
                 BeanUtils.copyProperties(order, orderVO);

                 // 3. 把刚才查出来的【具体菜品列表】塞进 VO 里
                 orderVO.setOrderDetailList(orderDetailList);

                 // 4. 把这单打包好的数据，加到最终要返回的 list 里
                 list.add(orderVO);
             }
         }
         //封装好总条数和列表，返回给前端展示
         return new PageResult(page1.getTotal(), list);

        }

    /**
     * 用户提交订单
     *
     * 整体流程分为5步：
     *   第1步：校验业务数据（地址簿是否存在、购物车是否为空）
     *   第2步：向订单表（orders）插入一条订单记录
     *   第3步：将购物车中的每一条商品，转为订单明细，批量插入订单明细表（order_detail）
     *   第4步：下单成功后，清空当前用户的购物车
     *   第5步：将订单的关键信息封装成 VO 对象，返回给前端展示
     *
     * @param ordersSubmitDTO 前端提交过来的订单数据（包含地址id、支付方式、备注等）
     * @return OrderSubmitVO 返回给前端的结果（订单id、订单号、金额、下单时间）
     */
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // ==================== 第1步：业务异常校验 ====================
        // 1.1 根据前端传来的地址id，查询地址簿，确认该地址是否存在
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 地址不存在，无法下单，直接抛出自定义业务异常
            // 全局异常处理器会捕获这个异常，并返回友好的错误提示给前端
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 1.2 查询当前登录用户的购物车数据
        // BaseContext.getCurrentId() 是从 ThreadLocal 中获取当前请求的用户id
        // ThreadLocal 可以保证同一个请求的多个方法共享同一个用户id，线程安全
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId); // 设置查询条件：只查当前用户的购物车
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            // 购物车为空，没有商品可以下单，抛出自定义业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // ==================== 第2步：向订单表插入1条数据 ====================
        // 创建一个新的订单对象
        Orders orders = new Orders();
        // BeanUtils.copyProperties 是 Spring 提供的工具类
        // 作用：将 ordersSubmitDTO 中同名属性的值，拷贝到 orders 对象中
        // 比如：addressBookId、payMethod、amount、remark 等都会自动拷贝
        BeanUtils.copyProperties(ordersSubmitDTO, orders);

        // 手动设置 DTO 中没有的字段
        orders.setOrderTime(LocalDateTime.now());           // 下单时间：取当前时间
        orders.setPayStatus(Orders.UN_PAID);                // 支付状态：未付款
        orders.setStatus(Orders.PENDING_PAYMENT);            // 订单状态：待付款
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // 订单号：用当前时间戳生成，保证唯一
        orders.setPhone(addressBook.getPhone());             // 收货人手机号：从地址簿中取
        orders.setConsignee(addressBook.getConsignee());     // 收货人姓名：从地址簿中取
        orders.setUserId(userId);                            // 下单用户id

        // 执行插入操作，MyBatis 会自动回填主键 id 到 orders 对象中
        orderMapper.insert(orders);

        // ==================== 第3步：向订单明细表插入n条数据 ====================
        // 购物车中有多少种商品，就生成多少条订单明细
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            // 为购物车中的每一条商品，创建一个对应的订单明细对象
            OrderDetail orderDetail = new OrderDetail();
            // 将购物车中商品的属性（名称、价格、数量、图片等）拷贝到订单明细中
            BeanUtils.copyProperties(cart, orderDetail);
            // 手动设置订单明细所属的订单id（上一步插入后自动回填的id）
            orderDetail.setOrderId(orders.getId());
            // 将当前订单明细加入到列表中
            orderDetailList.add(orderDetail);
        }
        // 批量插入所有订单明细，提高性能（一条SQL插入多条记录，而不是循环单条插入）
        orderDetailMapper.insertBatch(orderDetailList);

        // ==================== 第4步：清空当前用户的购物车 ====================
        // 下单成功后，购物车中的商品已经转为订单，需要清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // ==================== 第5步：封装VO返回结果 ====================
        // VO（View Object）：视图对象，专门用于返回给前端展示
        // 使用 Builder 模式（建造者模式）构建对象，链式调用，代码更简洁优雅
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())                         // 订单主键id
                .orderTime(orders.getOrderTime())           // 下单时间
                .orderAmount(orders.getAmount())            // 订单总金额
                .orderNumber(orders.getNumber())            // 订单号
                .build();                                   // 最终构建出 OrderSubmitVO 对象
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
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
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
    }

}