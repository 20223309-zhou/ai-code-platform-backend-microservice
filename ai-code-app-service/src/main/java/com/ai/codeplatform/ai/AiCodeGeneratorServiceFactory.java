package com.ai.codeplatform.ai;

import com.ai.codeplatform.ai.guardrail.PromptSafetyInputGuardrail;
import com.ai.codeplatform.ai.tools.*;
import com.ai.codeplatform.exception.BusinessException;
import com.ai.codeplatform.exception.ErrorCode;
import com.ai.codeplatform.ai.model.enums.CodeGenTypeEnum;
import com.ai.codeplatform.ai.model.enums.ModelEnum;
import com.ai.codeplatform.service.ChatHistoryOriginalService;
import com.ai.codeplatform.utils.SpringContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.Skills;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Resource
    private ToolManager toolManager;

    @Resource
    private RetrievalAugmentor retrievalAugmentor;

    @Resource
    private Skills skills;

    @Resource
    private SearchImageTool searchImageTool;

    @Resource
    private GenerateLogoTool generateLogoTool;
    /**
     * AI 服务实例缓存
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            //记录缓存淘汰日志
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）这个方法是为了兼容历史逻辑
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML, ModelEnum.DEEP_SEEK.getModelName());
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType, String modelName) {
        String cacheKey = buildCacheKey(appId, codeGenType, modelName);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType, modelName));
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType, String modelName) {
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                // 对话记忆最大条数
                // 注意：不能设得过大。RedisChatMemoryStore.updateMessages 是「每条消息全量 setex」，
                // 窗口越大单次写入体积越大（实测 200 条约 524KB），且每加一条都重写一次。
                // 200 条已远超 32K 上下文能容纳的量，再大只会加重 Redis 写入负担。
                .maxMessages(200)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryOriginalService.loadOriginalChatHistoryToMemory(appId, chatMemory);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = resolveStreamingChatModel(modelName);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .retrievalAugmentor(retrievalAugmentor)
                        .toolProvider(skills.toolProvider())
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        // 添加输入护轨
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();
            }
            case HTML -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = resolveStreamingChatModel(modelName);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(resolveChatModel(modelName))
                        .toolProvider(skills.toolProvider())
                        .streamingChatModel(openAiStreamingChatModel)
                        .retrievalAugmentor(retrievalAugmentor)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(new WebFetchTool(), new FileReadTool(),
                                new FileModifyTool(), searchImageTool, generateLogoTool)
                        // 添加输入护轨
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            case MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel multiStreamingChatModel = resolveStreamingChatModel(modelName);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(resolveChatModel(modelName))
                        .toolProvider(skills.toolProvider())
                        .streamingChatModel(multiStreamingChatModel)
                        .retrievalAugmentor(retrievalAugmentor)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(new WebFetchTool(), new FileReadTool(),
                                new FileModifyTool(), new FileWriteTool(),
                                searchImageTool, generateLogoTool)
                        // 添加输入护轨
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }


    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 按模型名解析流式模型（多例 prototype，解决并发问题）；未知模型回退 DeepSeek。
     */
    private StreamingChatModel resolveStreamingChatModel(String modelName) {
        return switch (resolveModelEnum(modelName)) {
            case DEEP_SEEK ->
                    SpringContextUtil.getBean("deepseekReasoningStreamingChatModelPrototype", StreamingChatModel.class);
            case GROK ->
                    SpringContextUtil.getBean("grokReasoningStreamingChatModelPrototype", StreamingChatModel.class);
            case CHAT_GPT ->
                    SpringContextUtil.getBean("chatgptReasoningStreamingChatModelPrototype", StreamingChatModel.class);
        };
    }

    /**
     * 按模型名解析非流式模型；未知模型回退 DeepSeek。
     */
    private ChatModel resolveChatModel(String modelName) {
        return switch (resolveModelEnum(modelName)) {
            case DEEP_SEEK -> SpringContextUtil.getBean("deepseekChatModelPrototype", ChatModel.class);
            case GROK -> SpringContextUtil.getBean("grokChatModelPrototype", ChatModel.class);
            case CHAT_GPT -> SpringContextUtil.getBean("chatgptChatModelPrototype", ChatModel.class);
        };
    }

    /**
     * 模型名 -> 枚举；为空或未知时回退默认模型 DeepSeek。
     */
    private ModelEnum resolveModelEnum(String modelName) {
        ModelEnum modelEnum = ModelEnum.getEnumByModelName(modelName);
        return modelEnum != null ? modelEnum : ModelEnum.DEEP_SEEK;
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType, String modelName) {
        return appId + "_" + codeGenType.getValue() + "_" + modelName;
    }

}
