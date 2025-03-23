package com.heller.lj.config;

import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * 使用 Redis 来存储对话的上下文
 *
 * 可以储存到任意地方，需要实现 ChatMemoryStore 接口
 *
 * TODO 实现具体功能
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final RedisTemplate redisTemplate;

    public RedisChatMemoryStore(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // TODO
        return null;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        // TODO
    }

    @Override
    public void deleteMessages(Object memoryId) {
        // TODO
    }

}
