package com.wjh.aicodegen.config;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 普通 Redis 聊天记忆存储。
 * LangChain4j 官方 RedisChatMemoryStore 依赖 RedisJSON，普通 Redis 会报 JSON.GET unknown command。
 *
 * @author 王哈哈
 */
@Configuration
@Data
public class RedisChatMemoryStoreConfig {

    private static final String MEMORY_KEY_PREFIX = "langchain4j:chat-memory:";

    @Value("${spring.data.redis.ttl}")
    private long ttl;

    @Bean
    public ChatMemoryStore redisChatMemoryStore(StringRedisTemplate stringRedisTemplate) {
        return new ChatMemoryStore() {
            @Override
            public List<ChatMessage> getMessages(Object memoryId) {
                String json = stringRedisTemplate.opsForValue().get(buildMemoryKey(memoryId));
                if (StrUtil.isBlank(json)) {
                    return List.of();
                }
                return ChatMessageDeserializer.messagesFromJson(json);
            }

            @Override
            public void updateMessages(Object memoryId, List<ChatMessage> messages) {
                String key = buildMemoryKey(memoryId);
                String json = ChatMessageSerializer.messagesToJson(messages);
                if (ttl > 0) {
                    stringRedisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
                } else {
                    stringRedisTemplate.opsForValue().set(key, json);
                }
            }

            @Override
            public void deleteMessages(Object memoryId) {
                stringRedisTemplate.delete(buildMemoryKey(memoryId));
            }
        };
    }

    private String buildMemoryKey(Object memoryId) {
        return MEMORY_KEY_PREFIX + memoryId;
    }
}
