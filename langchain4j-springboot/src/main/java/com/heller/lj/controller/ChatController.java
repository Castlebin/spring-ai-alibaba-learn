package com.heller.lj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    private QwenChatModel qwenChatModel;

    /**
     * 访问地址：http://localhost:8080/ai/chat?message=写首诗？
     */
    @RequestMapping("/chat")
    public String chat(@RequestParam(defaultValue = "你是谁？") String message) {
        String response = qwenChatModel.chat(message);
        return response;
    }

}
