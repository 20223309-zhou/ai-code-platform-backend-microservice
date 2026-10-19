package com.ai.codeplatform.service;

import com.mybatisflex.core.service.IService;
import com.ai.codeplatform.ai.model.entity.ChatHistoryOriginal;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 原始对话历史 服务层。
 * 为 vue 工程模式恢复对话记忆(包含工具调用信息)
 *
 * @author agx
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {


    /**
     * 添加对话历史
     *
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 批量添加对话历史
     *
     * @param chatHistoryOriginalList
     * @return
     */
    boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList);

    /**
     * 根据 appId 关联删除对话历史记录
     *
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 将 APP 的对话历史加载到缓存中。
     * <p>
     * 按 token 预算回放（见 {@code app.chat-history} 配置），保证语义主线不丢。
     *
     * @param appId      应用 id
     * @param chatMemory 对话记忆
     * @return 实际加载的 record 数
     */
    int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory);

    /**
     * 将 APP 的对话历史加载到缓存中（可指定本次扫描的记录上限）
     *
     * @param appId     应用 id
     * @param chatMemory 对话记忆
     * @param scanLimit  扫描条数上限
     * @return 实际加载的 record 数
     */
    int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int scanLimit);
}


