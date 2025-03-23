package com.heller.lj.start;

import jdk.jfr.Description;

// 定义任务类型
public enum TaskType {
    @Description("普通对话，聊天")
    CHAT,
    @Description("编程助手")
    CODING,
    @Description("生成图片")
    GEN_IMAGE,
    @Description("其他任务")
    OTHER,
}
