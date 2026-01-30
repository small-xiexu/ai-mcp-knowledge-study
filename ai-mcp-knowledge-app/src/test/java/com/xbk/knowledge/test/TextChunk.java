package com.xbk.knowledge.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文本块实体类
 * 用于表示文档分割后的文本片段，支持父子关系（用于层级化检索）
 * 每个文本块包含唯一 ID、文本内容、父块 ID 和元数据
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

    /**
     * 文本块唯一标识，默认使用 UUID 生成
     */
    private String id;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 父文本块 ID，用于建立层级关系
     * 在 RAG 场景中可实现"先检索子块，再扩展到父块"的策略
     */
    private String parentId;

    /**
     * 元数据，存储额外信息（如来源、页码、知识库标签等）
     */
    private Map<String, Object> metadata;

    /**
     * 简单构造器：仅指定文本内容
     * 自动生成 UUID 作为 ID，并初始化空的元数据 Map
     *
     * @param text 文本内容
     */
    public TextChunk(String text) {
        this
                .id = UUID
                .randomUUID()
                .toString();
        this.text = text;
        this.metadata = new HashMap<>();
    }
}
