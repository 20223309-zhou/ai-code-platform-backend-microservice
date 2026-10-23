package com.ai.codeplatform.rag.config;

import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地 ONNX 向量模型（BGE-small-zh-v1.5，512 维）。
 * ⚠️ 该模型在构造时就会加载 onnxruntime 的原生库（libonnxruntime.so），
 * 而当前 jar 里带的是 onnxruntime 1.20，要求 **glibc ≥ 2.27**。
 * 在 CentOS 7（glibc 2.17）这类老系统上会直接抛：
 * <pre>
 * UnsatisfiedLinkError: libonnxruntime.so: /lib64/libm.so.6: version `GLIBC_2.27' not found
 * </pre>
 * 由于 RAG 链是「embeddingModel → retriever → augmentor → aiCodeGeneratorServiceFactory」
 * 的硬依赖，一个 so 会让整个 app-service 起不来。
 * <p>
 * 所以整条 RAG 链统一受 {@code app.rag.enabled} 控制：
 * <ul>
 *   <li>默认 {@code true}，本地开发行为完全不变</li>
 *   <li>不满足 glibc 条件的机器用 {@code --app.rag.enabled=false} 启动，
 *       服务可用但没有知识库检索（生成质量略有下降，功能不受影响）</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingModelConfig {

    @Bean
    public BgeSmallZhV15EmbeddingModel embeddingModel() {
        return new BgeSmallZhV15EmbeddingModel();
    }

}
