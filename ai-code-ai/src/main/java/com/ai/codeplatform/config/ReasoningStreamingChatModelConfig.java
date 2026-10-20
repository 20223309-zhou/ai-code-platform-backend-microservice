package com.ai.codeplatform.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

/**
 * 推理模型配置（多模型）。
 * <p>
 * DeepSeek 走官方地址；Grok / ChatGPT 走第三方聚合网关（bblabu）。
 * 所有模型均为 prototype：每次生成取独立实例，避免多请求共用同一 ChatModel。
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String deepseekBaseUrl;

    private String deepseekApiKey;

    private String deepseekModelName;

    /**
     * Grok / ChatGPT 共用的聚合网关地址
     */
    private String bblabuBaseUrl;

    private String grokApiKey;
    private String grokModelName;

    private String chatgptApiKey;
    private String chatgptModelName;

    private Integer maxTokens;

    private Double temperature;

    private Boolean logRequests = false;

    private Boolean logResponses = false;

    // ==================== 流式模型 ====================

    @Bean
    @Scope("prototype")
    public StreamingChatModel deepseekReasoningStreamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .accumulateToolCallId(false)
                .sendThinking(true)
                .returnThinking(true)
                .apiKey(deepseekApiKey)
                .baseUrl(bblabuBaseUrl)
                .modelName(deepseekModelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    @Bean
    @Scope("prototype")
    public StreamingChatModel grokReasoningStreamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .accumulateToolCallId(false)
                .sendThinking(true)
                .returnThinking(true)
                .apiKey(grokApiKey)
                .baseUrl(bblabuBaseUrl)
                .modelName(grokModelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    @Bean
    @Scope("prototype")
    public StreamingChatModel chatgptReasoningStreamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .accumulateToolCallId(false)
                .sendThinking(true)
                .returnThinking(true)
                .apiKey(chatgptApiKey)
                .baseUrl(bblabuBaseUrl)
                .modelName(chatgptModelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    // ==================== 非流式 ChatModel ====================
    // 供 generateHtmlCode / generateMultiFileCode 等非流式方法使用，同样按模型区分

    @Bean
    @Scope("prototype")
    public ChatModel deepseekChatModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(bblabuBaseUrl)
                .modelName(deepseekModelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    @Bean
    @Scope("prototype")
    public ChatModel grokChatModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(grokApiKey)
                .baseUrl(bblabuBaseUrl)
                .modelName(grokModelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    @Bean
    @Scope("prototype")
    public ChatModel chatgptChatModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(chatgptApiKey)
                .baseUrl(bblabuBaseUrl)
                .modelName(chatgptModelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofMinutes(3))
                .build();
    }
}
