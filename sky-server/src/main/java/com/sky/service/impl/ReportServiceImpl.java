package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
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
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间内的营业额统计数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //封装TurnoverReportVO
        //1.存放日期范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            //日期计算并添加
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        //List集合存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            //2.查询日期对应的营业额（营业额是当天“已完成”的订单的营业额总和
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            //查询语句 select sum(amount) from orders where order_time > beginTime and order_time< endTime and status = 5
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover==null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        //封装返回结果
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 统计指定时间内的用户统计数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //封装UserReportVO
        //1.存放日期范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            //日期计算并添加
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        //2.存放每天的用户总量 select count(id) from user where create time< ?
        List<Integer> totalUserList = new ArrayList<>();
        //3.存放每天的新增用户量：select count(id) from user where create time> ? and create time< ?
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);

            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 统计指定时间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //封装OrderReportVO
        //1.日期，以逗号分隔，例如：2022-10-01,2022-10-02,2022-10-03
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            //日期计算并添加
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        //2.每日订单数，以逗号分隔，例如：260,210,215
        List<Integer> orderCountList = new ArrayList<>();
        //3.每日有效订单数(订单状态为已完成的），以逗号分隔，例如：20,21,10
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            // 查询总订单数
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer orders = orderMapper.countByMap(map);

            // 查询有效订单数
            map.put("status", Orders.COMPLETED);
            Integer validOrders = orderMapper.countByMap(map);

            orderCountList.add(orders);
            validOrderCountList.add(validOrders);
        }

        //4.订单总数（采用stream流）
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //5.有效订单总数（采用stream流）
        Integer totalValidOrderCount=validOrderCountList.stream().reduce(Integer::sum).get();

        //6.订单完成率(要判断totalOrderCount是否为0，以防出现数学错误）
        Double orderCompletionRate=0.0;
        if (totalOrderCount!=0){
            orderCompletionRate= totalValidOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 获取订单销量前10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据--最近30天的运营数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);
        //2.通过POI将数据写入到Excel文件中
        //(1)传输入流来读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/BusinessStatisticsExcel.xlsx");
        try {
            //（2）基于模板文件创建新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //(3) 填充数据-时间
            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            sheet1.getRow(1).getCell(1).setCellValue("时间："+begin+"至"+end);
            //(4) 填充数据-概览数据
            //①营业额 turnover
            sheet1.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            //②订单完成率 orderCompletionRate
            sheet1.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            //②新增用户数 newUsers
            sheet1.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());
            //④有效订单 validOrderCount
            sheet1.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            //⑤平均客单价 unitPrice
            sheet1.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            //(5)填充数据—明细数据
            //①生成日期List
            //②填入数据
            for (int i=0;i<30;i++) {
                //时间设置
                LocalDate localDate = begin.plusDays(i);
                LocalDateTime timeOfBegin = LocalDateTime.of(localDate, LocalTime.MIN);
                LocalDateTime timeOfEnd = LocalDateTime.of(localDate, LocalTime.MAX);
                BusinessDataVO businessDataOfDay = workspaceService.getBusinessData(timeOfBegin, timeOfEnd);
                //填写数据
                sheet1.getRow(7+i).getCell(1).setCellValue(localDate.toString());
                sheet1.getRow(7+i).getCell(2).setCellValue(businessDataOfDay.getTurnover());
                sheet1.getRow(7+i).getCell(3).setCellValue(businessDataOfDay.getValidOrderCount());
                sheet1.getRow(7+i).getCell(4).setCellValue(businessDataOfDay.getOrderCompletionRate());
                sheet1.getRow(7+i).getCell(5).setCellValue(businessDataOfDay.getUnitPrice());
                sheet1.getRow(7+i).getCell(6).setCellValue(businessDataOfDay.getNewUsers());
            }

            //3.通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //4.关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
