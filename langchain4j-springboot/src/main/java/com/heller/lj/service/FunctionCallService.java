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
     * @param region
     * @param name
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

}
