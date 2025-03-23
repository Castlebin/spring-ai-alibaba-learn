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
    private EmbeddingStore embeddingStore;
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private Assistant assistant;
    @Autowired
    private AssistantUnique assistantUnique;
    @Autowired
    private AssistantUniqueRedis assistantUniqueRedis;

    @Autowired
    private AiRAGAssitant aiRAGAssitant;

    @PostConstruct
    public void init() {
        initRAGStore();
    }

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
     * 访问地址：http://localhost:8080/ai/chat-rag?message=非自愿退票？
     */
    @RequestMapping("/chat-rag")
    public Flux<String> streamChatRAG(@RequestParam(defaultValue = "你是谁？") String message,
            @RequestParam(defaultValue = "1") String memoryId) {
        TokenStream tokenStream = aiRAGAssitant.chatStream(memoryId, message);

        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> sink.next(partialResponse))
                    .onCompleteResponse(completeResponse -> {
                        sink.complete();
                    }).onError(sink::error)
                    .start();
        });
    }

    /**
     * RAG 数据库的初始化，从文件系统中加载了原始知识库数据，存放到向量数据库中
     * 简单的演示了一下向量数据库的使用，使用的是 InMemoryEmbeddingStore （内存中）
     */
    public void initRAGStore() {
        // 从文件系统中加载原始知识库数据
        List<Document> documents = getKnowledge();
        // 使用分割器将文档 分割 成多个文本片段
        List<TextSegment> segments = splitDocuments(documents);

        // 0. 利用 向量模型（Embedding 模型） 将知识库转化为 Embedding 向量，存放到向量数据库中
        // EMBEDDING_STORE.addAll(EMBEDDING_MODEL.embedAll(segments).content());  (可以直接使用这句将全部数据转化为向量并添加到向量数据库中)
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
    }

    /**
     * 使用文件系统作为知识库数据来源
     */
    private static List<Document> getKnowledge() {
        // 使用 ClassPathDocumentLoader 来加载文档，使用 TextDocumentParser 来解析文档
        DocumentParser documentParser = new TextDocumentParser();
        List<Document> documents = ClassPathDocumentLoader.loadDocumentsRecursively("docs/", documentParser);
        return documents;
    }

    private static List<TextSegment> splitDocuments(List<Document> documents) {
        // 使用 某个文本分割器 来将文档分割成多个文本片段 （分块  chunk ）
        // 这里我们使用的是 DocumentByRegexSplitter 来分割文档
        DocumentByRegexSplitter splitter = new DocumentByRegexSplitter(
                "\\n\\d+\\.", // 正则表达式
                "\n",  // 保留换行符
                120, // 每个文本片段的最大长度
                10, // 允许重叠的最大字符数
                new DocumentByCharacterSplitter(100, 20) // 子分割器
        );

        List<TextSegment> result = new ArrayList<>();
        for (Document document : documents) {
            List<TextSegment> segments = splitter.split(document);
            result.addAll(segments);
        }
        return result;
    }

}
