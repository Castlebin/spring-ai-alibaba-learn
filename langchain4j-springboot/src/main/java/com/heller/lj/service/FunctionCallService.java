package com.heller.lj.service;

import org.springframework.stereotype.Service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

/**
 * 定义各种 Function Call 服务
 */
@Slf4j
@Service
public class FunctionCallService {

    /**
     * 统计某个地区有多少个人叫某个名字
     *
     * @return 个数
     */
    // @Tool 注解可以定义一个 Function Call 工具
    @Tool("{地区}有多少个人叫{name}？")
    public Integer countNames(
            // @P 告诉 AI 要提取的信息
            @P("地区")
            String region,
            @P("姓名")
            String name
    ) {
        log.info("调用了 countNames 方法，地区：{}，姓名：{}", region, name);

        // 这里可以调用数据库或者其他服务来获取数据
        // 这里简单模拟一下
        if ("北京".equals(region) && "张三".equals(name)) {
            return 100;
        } else if ("上海".equals(region) && "李四".equals(name)) {
            return 200;
        } else {
            return 5;
        }
    }

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     * @param name 姓名
     * @return 取消结果
     */
    @Tool(name = "取消订单",
            value = {"取消订单", "退票", "退订"})
    public String cancelBooking(
            @P("订单号") String orderNo,
            @P("姓名") String name
    ) {
        log.info("调用了 cancelBooking 方法，订单号：{}，姓名：{}", orderNo, name);

        // 这里可以操作数据库或者调用其他服务来取消订单
        // 这里简单模拟一下
        if ("123456".equals(orderNo) && "张三".equals(name)) {
            return "取消成功";
        } else if ("654321".equals(orderNo) && "李四".equals(name)) {
            return "取消失败";
        } else {
            return "订单不存在";
        }
    }

}
