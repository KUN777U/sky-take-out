package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 统计订单营业额
     * @param begin
     * @param end
     * @return
     *
     * 这个方法：先列出从开始到结束的每一天，在逐天查询已完成的订单的金额总和，
     * 最后把日期和营业额分别拼成字符串，放进TurnoverReportVO返回给前端画图使用
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);  //从begin一直加到end，得到每一天
        }

        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map<String,Object> map = new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover; //判断turnover是否为空，空则赋值为0.0，再重新赋值给他自己
            turnoverList.add(turnover);
        }
        //日期列表，用逗号分隔拼成字符串，方便前端使用
        String dateListStr = StringUtils.join(dateList,",");
        String turnoverListStr = StringUtils.join(turnoverList,",");

        //用Builder()方法创建了TurnoverReportVO对象,设置dateList属性和turnoverList属性为字符串格式
        return TurnoverReportVO
                .builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build(); //.build() 生成最终对象
    }

    /**
     * 统计指定时间区间内的用户数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);  //从begin一直加到end，得到每一天
        }

        //新增用户列表
        List<Integer> newUserList = new ArrayList<>();
        //用户总量列表
        List<Integer> totalUserList = new ArrayList<>();
        //遍历每一天
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String,Object> map = new HashMap<>();
            map.put("end",endTime);
            //查询总用户数量
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin",beginTime);

            //查询新增用户数量
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        //日期列表，将list集合转换为字符串，用逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");
        String totalUserListStr = StringUtils.join(totalUserList, ",");
        String newUserListStr = StringUtils.join(newUserList, ",");

        //封装UserReportVO对象
        return UserReportVO
                .builder()
                .dateList(dateListStr)
                .totalUserList(totalUserListStr)
                .newUserList(newUserListStr)
                .build();
    }
}