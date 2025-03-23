package com.heller.lj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

@Configuration
public class AiAssistant {

    public interface Assistant {
        String chat(String message); // 普通的对话

        TokenStream chatStream(String message); // 流式响应的对话
    }

    @Bean
    public Assistant assistant(ChatLanguageModel qwenChatModel, StreamingChatLanguageModel qwenStreamingChatModel) {
        // 使用 ChatMemory (存储对话的上下文，默认是在内存中)
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10); // 设置 最多存储 10 条对话

        Assistant assistant = AiServices.builder(Assistant.class)  // 帮助生成一个 Assistant 的动态代理
                .chatLanguageModel(qwenChatModel)
                .streamingChatLanguageModel(qwenStreamingChatModel)
                .chatMemory(chatMemory) // 设置对话的上下文，使用 ChatMemory 来保存对话的上下文
                .build();

        return assistant;
    }

}
