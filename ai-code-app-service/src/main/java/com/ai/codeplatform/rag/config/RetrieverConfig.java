package com.ai.codeplatform.rag.config;

import cn.hutool.core.util.StrUtil;
import com.ai.codeplatform.Interceptor.RagSwitchHolder;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 向量检索器。整个类受 {@code app.rag.enabled} 控制；
 * 关闭时不会创建 retriever，也就不会触发 embeddingModel 的原生库加载。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RetrieverConfig {
    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;
    @Resource
    private BgeSmallZhV15EmbeddingModel embeddingModel;

    @Bean
    public EmbeddingStoreContentRetriever retriever() {
        return EmbeddingStoreContentRetriever.builder()
                /*
                 * 按「当前应用的技术栈」过滤：知识库混存所有项目的代码，
                 * 不过滤会让 React 项目检索到 Vue 代码当参考，把 AI 带偏。
                 * 原来靠 query.text().contains("vue") 猜，用户不提框架（"做个音乐播放器"）
                 * 或写大写 "Vue" 就失效，所以改为优先读请求上下文里的真实类型。
                 */
                .dynamicFilter(query -> {
                    String projectType = RagSwitchHolder.getProjectType();
                    if (StrUtil.isNotBlank(projectType)) {
                        return metadataKey("project_type").isEqualTo(projectType);
                    }
                    // 兜底：拿不到上下文时退回文本猜测
                    String text = query.text() == null ? "" : query.text().toLowerCase();
                    if (text.contains("vue")) {
                        return metadataKey("project_type").isEqualTo("vue");
                    }
                    if (text.contains("react")) {
                        return metadataKey("project_type").isEqualTo("react");
                    }
                    if (text.contains("html")) {
                        return metadataKey("project_type").isEqualTo("html");
                    }
                    if (text.contains("multi")) {
                        return metadataKey("project_type").isEqualTo("multi");
                    }
                    return null;
                })
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.75)
                .displayName("retriever")
                .build();
    }
}
