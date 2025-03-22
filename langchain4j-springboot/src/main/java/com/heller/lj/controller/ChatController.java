package com.heller.lj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    private QwenChatModel qwenChatModel;

    @Autowired
    private QwenStreamingChatModel qwenStreamingChatModel;

    /**
     * 使用普通响应
     * 访问地址：http://localhost:8080/ai/chat?message=写首诗？
     */
    @RequestMapping("/chat")
    public String chat(@RequestParam(defaultValue = "你是谁？") String message) {
        String response = qwenChatModel.chat(message);
        return response;
    }

    /**
     * 使用流式响应
     * 访问地址：http://localhost:8080/ai/stream?message=写首诗？
     */
    @RequestMapping("/stream")
    public Flux<String> stream(@RequestParam(defaultValue = "你是谁？") String message) {
        Flux<String> response = Flux.create(sink -> qwenStreamingChatModel.chat(message,
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        sink.next(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        sink.error(throwable);
                    }
                }));
        return response;
    }

}
