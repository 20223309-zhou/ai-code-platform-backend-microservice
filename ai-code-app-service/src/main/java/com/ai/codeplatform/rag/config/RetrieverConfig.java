package com.ai.codeplatform.rag.config;

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
                .dynamicFilter(query -> {
                    // 根据用户查询动态生成过滤条件
                    if (query.text().contains("vue")) {
                        return metadataKey("project_type").isEqualTo("vue");
                    }else if (query.text().contains("html")){
                        return metadataKey("project_type").isEqualTo("html");
                    }else if (query.text().contains("multi")){
                        return metadataKey("project_type").isEqualTo("multi");
                    }
                    return null;  // 不过滤
                })
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.75)
                .displayName("retriever")
                .build();
    }
}
