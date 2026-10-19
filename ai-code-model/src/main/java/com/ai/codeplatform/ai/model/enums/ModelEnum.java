package com.ai.codeplatform.ai.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 可选的大模型。
 * <p>
 * provider 用于按业务语义区分（如前端展示、配置映射），modelName 是与模型服务商约定的模型标识。
 */
@Getter
public enum ModelEnum {
    GROK("grok", "grok-4.7", "Grok-4.7"),
    DEEP_SEEK("deepseek", "deepseek-flash", "DeepSeek-V4.1-Flash"),
    CHAT_GPT("chatgpt", "gpt-6.1-sol", "ChatGPT-6.1-Sol");

    private final String provider;
    private final String modelName;
    private final String label;

    ModelEnum(String provider, String modelName, String label) {
        this.provider = provider;
        this.modelName = modelName;
        this.label = label;
    }

    /**
     * 根据 provider 获取枚举
     *
     * @param provider 枚举值的provider
     * @return 枚举值
     */
    public static ModelEnum getEnumByProvider(String provider) {
        if (ObjUtil.isEmpty(provider)) {
            return null;
        }
        for (ModelEnum anEnum : ModelEnum.values()) {
            if (anEnum.provider.equals(provider)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据 modelName 获取枚举
     *
     * @param modelName 枚举值的modelName
     * @return 枚举值
     */
    public static ModelEnum getEnumByModelName(String modelName) {
        if (ObjUtil.isEmpty(modelName)) {
            return null;
        }
        for (ModelEnum anEnum : ModelEnum.values()) {
            if (anEnum.modelName.equals(modelName)) {
                return anEnum;
            }
        }
        return null;
    }
}
