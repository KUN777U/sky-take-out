package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

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


    /**
     * 统计订单用户数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);  //从begin一直加到end，得到每一天
        }
        //存放每天的订单总数的集合
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天的有效订单数的集合
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天的订单总数：select count(id) from orders where create_time > ? and create_time < ?
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            //查询每天的有效订单数：select count(id) from orders where create_time > ? and create_time < ? and status = 5
            Integer validOrderCount = getOrderCount(beginTime,endTime,Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);

        }
        //计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get(); //orderCountList.stream().reduce(Integer::sum)把列表中的数全部加起来，.get() 是因为 reduce 返回 Optional<Integer>（可能为空），这里取出来
        //计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //Stringutils.join(列表,",")把[08-01,08-02,08-03]拼成"2026-08-01,2026-08-02,2026-08-03"
        String dateListStr = StringUtils.join(dateList,",");
        String orderCountListStr = StringUtils.join(orderCountList,",");
        String validOrderCountListStr = StringUtils.join(validOrderCountList, ",");

        //计算订单完成率
        Double orderCompletionRate = 0.0;  //先判断订单总数是否为0，避免除0异常
        if(totalOrderCount > 0){ //.doubleValue()是为了做小数除法，将Integer转换为Double，如果两个Integer直接相除，java会做整数除法，完成率永远是0
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }
        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(dateListStr) //日期列表
                .orderCountList(orderCountListStr) //总订单数列表
                .validOrderCountList(validOrderCountListStr) //有效订单数列表
                .totalOrderCount(totalOrderCount) //订单总数
                .validOrderCount(validOrderCount) //有效订单数
                .orderCompletionRate(orderCompletionRate)  //订单完成率
                .build();
        return orderReportVO;
    }



    /**
     * 根据条件统计订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map<String,Object> map = new HashMap<>();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }


    /**
     * 统计指定时间区间内的销量排名Top10
     * @param begin
     * @param end
     * @return   select od.name,sum(od.number) number from order_detail od orders o where od.order_id = o.id
     *           and o.status = 5 and o.order_time > ? and o.order_time < ?
     *           group by od.name order by number desc limit 10
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        //用stream()流从查询结果中提取所有商品名称，拼成逗号分隔的字符串，如："鱼香肉丝","宫保鸡丁","麻婆豆腐"。
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameStr = StringUtils.join(names, ",");
        //用stream()流从查询结果中提取所有商品销量，拼成逗号分隔的字符串，如："100,200,300"。
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberStr = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameStr)
                .numberList(numberStr)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据--查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);  //从今天往前推30天
        LocalDate dateEnd = LocalDate.now().minusDays(1);  //从今天往前退1天，也就是昨天

        //日期LocalDate转换为LocalDateTime
        LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(dateEnd, LocalTime.MAX);
        //查询出概览数据
        BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);

        //通过输入流读取数据
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表.xlsx");
        //2.通过POI将数据写入Excel文件中
        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的第一个sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间" + begin + "至" + end);

            //获取第4行数据
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(3).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(4).setCellValue(businessData.getNewUsers());

            //获取第五行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(3).setCellValue(businessData.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的数据
                LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
                BusinessDataVO businessData1 = workspaceService.getBusinessData(beginTime, endTime);

                //获得某一行
                row = sheet.getRow(i+7);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData1.getTurnover());
                row.getCell(3).setCellValue(businessData1.getValidOrderCount());
                row.getCell(4).setCellValue(businessData1.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData1.getUnitPrice());
                row.getCell(6).setCellValue(businessData1.getNewUsers());
            }


            //通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}