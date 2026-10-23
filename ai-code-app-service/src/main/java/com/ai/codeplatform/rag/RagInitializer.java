package com.ai.codeplatform.rag;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.ai.codeplatform.constant.RagConstant.RAG_LOAD_DIRECTORY_PATH;

/**
 * 启动时把知识库目录灌入 Qdrant。受 {@code app.rag.enabled} 控制，
 * 关闭 RAG 时不会在启动阶段做向量化（也不会加载 ONNX 原生库）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagInitializer {

    @Resource
    private QdrantDocumentLoader qdrantDocumentLoader;

    @PostConstruct
    public void init() {
        log.info("初始化 RAG 系统...");
        
        Path path = Paths.get(RAG_LOAD_DIRECTORY_PATH);
        if (!path.toFile().exists()) {
            log.warn("文档目录不存在: {}", RAG_LOAD_DIRECTORY_PATH);
            return;
        }
        try {
            qdrantDocumentLoader.loadDocuments(RAG_LOAD_DIRECTORY_PATH);
            log.info("RAG 系统初始化完成");
        } catch (Exception e) {
            log.error("RAG 系统初始化失败", e);
        }
    }
}
