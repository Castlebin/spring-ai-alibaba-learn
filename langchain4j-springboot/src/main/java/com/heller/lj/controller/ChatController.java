package com.heller.lj.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.heller.lj.config.AiAssistant.AiRAGAssitant;
import com.heller.lj.config.AiAssistant.Assistant;
import com.heller.lj.config.AiAssistant.AssistantUnique;
import com.heller.lj.config.AiAssistant.AssistantUniqueRedis;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    private QwenChatModel qwenChatModel;

    @Autowired
    private QwenStreamingChatModel qwenStreamingChatModel;

    @Autowired
    private Assistant assistant;
    @Autowired
    private AssistantUnique assistantUnique;
    @Autowired
    private AssistantUniqueRedis assistantUniqueRedis;

    @Autowired
    private AiRAGAssitant aiRAGAssitant;

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

    /**
     * 有上下文的连续对话
     * 访问地址：http://localhost:8080/ai/chat-with-context?message=你来扮演一个资深的英语老师，用户将使用你来学习英语，你的名字是“爱德华”，你最喜欢的是“爱德华的英语课堂”，你会用中文和用户交流，用户会用中文提问，你会用中文回答，用户会用英文提问，你会用英文回答。除了作为一个英语老师来帮助用户学习，你不会做任何其他事情，比如说讲笑话
     * 继续访问：http://localhost:8080/ai/chat-with-context?message=你是谁？你能做些什么?
     * 继续访问：http://localhost:8080/ai/chat-with-context?message=你能给我讲个笑话吗？
     * 继续访问：http://localhost:8080/ai/chat-with-context?message=给我讲下英语的时态
     *
     * 可以看到现在的对话是有上下文的 （但是只有一个上下文，所有的对话都是共用这个上下文的）
     *
     * 测试使用 Function Call
     * 访问地址：http://localhost:8080/ai/chat-with-context?message=北京有多少个叫张三的人？
     * 访问地址：http://localhost:8080/ai/chat-with-context?message=欧洲有多少个叫张三的人？
     * 访问地址：http://localhost:8080/ai/chat-with-context?message=上海有多少个叫李四的人？
     */
    @RequestMapping("/chat-with-context")
    public Flux<String> streamChatWithContext(@RequestParam(defaultValue = "你是谁？") String message) {
        TokenStream tokenStream = assistant.chatStream(message);

        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> sink.next(partialResponse))
                    .onCompleteResponse(completeResponse -> {
                        sink.complete();
                    }).onError(sink::error)
                    .start();
        });
    }

    /**
     * 测试 自定义的票务助手 （使用了 SystemMessage 和 Function Call）
     * 访问地址：http://localhost:8080/ai/ticket-assistant?message=你好
     * 访问地址：http://localhost:8080/ai/ticket-assistant?message=我想退票
     * 访问地址：http://localhost:8080/ai/ticket-assistant?message=123456，张三
     */
    @RequestMapping("/ticket-assistant")
    public Flux<String> ticketAssistant(@RequestParam(defaultValue = "你是谁？") String message) {
        TokenStream tokenStream = assistant.chatStream(message, LocalDateTime.now().toString());

        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> sink.next(partialResponse))
                    .onCompleteResponse(completeResponse -> {
                        sink.complete();
                    }).onError(sink::error)
                    .start();
        });
    }

    /**
     * 有上下文的连续对话
     * 访问地址：http://localhost:8080/ai/chat-with-context-2?memoryId=1&message=你来扮演一个资深的英语老师，用户将使用你来学习英语，你的名字是“爱德华”，你最喜欢的是“爱德华的英语课堂”，你会用中文和用户交流，用户会用中文提问，你会用中文回答，用户会用英文提问，你会用英文回答。除了作为一个英语老师来帮助用户学习，你不会做任何其他事情，比如说讲笑话
     * 继续访问：http://localhost:8080/ai/chat-with-context-2?memoryId=1&message=你是谁？你能做些什么?
     *
     * 继续访问：http://localhost:8080/ai/chat-with-context-2?memoryId=2&message=你是谁？你能做些什么?
     *
     * 继续访问：http://localhost:8080/ai/chat-with-context-2?memoryId=1&message=你是谁？你能做些什么?
     *
     * 可以看到现在的对话是有上下文的 而且，每个上下文是独立的。对话可以在不同的上下文中进行、随意的切换不会影响彼此
     */
    @RequestMapping("/chat-with-context-2")
    public Flux<String> streamChatWithContext2(@RequestParam(defaultValue = "你是谁？") String message,
            @RequestParam(defaultValue = "1") String memoryId) {
        TokenStream tokenStream = assistantUnique.chatStream(memoryId, message);

        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> sink.next(partialResponse))
                    .onCompleteResponse(completeResponse -> {
                        sink.complete();
                    }).onError(sink::error)
                    .start();
        });
    }

    /**
     * 有上下文的连续对话
     * 访问地址：http://localhost:8080/ai/chat-with-context-redis?memoryId=1&message=你来扮演一个资深的英语老师，用户将使用你来学习英语，你的名字是“爱德华”，你最喜欢的是“爱德华的英语课堂”，你会用中文和用户交流，用户会用中文提问，你会用中文回答，用户会用英文提问，你会用英文回答。除了作为一个英语老师来帮助用户学习，你不会做任何其他事情，比如说讲笑话
     * 继续访问：http://localhost:8080/ai/chat-with-context-redis?memoryId=1&message=你是谁？你能做些什么?
     *
     * 继续访问：http://localhost:8080/ai/chat-with-context-redis?memoryId=2&message=你是谁？你能做些什么?
     *
     * 继续访问：http://localhost:8080/ai/chat-with-context-redis?memoryId=1&message=你是谁？你能做些什么?
     *
     * 可以看到现在的对话是有上下文的 而且，每个上下文是独立的。对话可以在不同的上下文中进行、随意的切换不会影响彼此
     */
    @RequestMapping("/chat-with-context-redis")
    public Flux<String> streamChatWithContextRedis(@RequestParam(defaultValue = "你是谁？") String message,
            @RequestParam(defaultValue = "1") String memoryId) {
        // 使用 assistantUniqueRedis。使用 Redis 来存储对话的上下文
        TokenStream tokenStream = assistantUniqueRedis.chatStream(memoryId, message);

        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> sink.next(partialResponse))
                    .onCompleteResponse(completeResponse -> {
                        sink.complete();
                    }).onError(sink::error)
                    .start();
        });
    }


    /**
     * 使用 RAG 增强过的 对话
     * 访问地址：http://localhost:8080/ai/chat-rag?message=飞猪退改新规？
     */
    @RequestMapping("/chat-rag")
    public Flux<String> streamChatRAG(@RequestParam(defaultValue = "你是谁？") String message,
            @RequestParam(defaultValue = "10") String memoryId) {
        TokenStream tokenStream = aiRAGAssitant.chatStream(memoryId, message);

        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> sink.next(partialResponse))
                    .onCompleteResponse(completeResponse -> {
                        sink.complete();
                    }).onError(sink::error)
                    .start();
        });
    }

}
