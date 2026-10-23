package com.ai.codeplatform.rag.config;

import com.ai.codeplatform.Interceptor.RagSwitchHolder;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 带请求级开关的检索器包装。受 {@code app.rag.enabled} 控制，
 * 关闭时整个 bean 不创建（它硬依赖 retriever，retriever 不存在就会注入失败）。
 */
@Component
@ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConditionalContentRetriever implements ContentRetriever {

    @Resource
    private EmbeddingStoreContentRetriever retriever;  // 真正的检索器

    @Override
    public List<Content> retrieve(Query query) {
        // 从当前请求上下文或应用中拿开关状态
        if (RagSwitchHolder.isEnabled()) {
            return retriever.retrieve(query);
        }
        return List.of();  // 关闭 RAG 时返回空
    }
}