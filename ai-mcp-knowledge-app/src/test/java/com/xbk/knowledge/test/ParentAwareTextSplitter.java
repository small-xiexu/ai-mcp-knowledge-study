package com.xbk.knowledge.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 父子关系感知的文本分割器
 * 实现层级化文档分割策略，支持"父块-子块"结构
 * 适用于 RAG 场景中的"子块检索 + 父块扩展"策略，提升上下文完整性
 *
 * <p>分割策略：
 * <ol>
 *   <li>第一轮：按最高优先级分隔符（如段落）分割为父块</li>
 *   <li>第二轮：对每个父块按次级分隔符（如句子）分割为子块</li>
 *   <li>超长子块按字符长度进一步分割</li>
 * </ol>
 *
 * @author xiexu
 */
public class ParentAwareTextSplitter {

    /**
     * 目标 chunk 大小（按字符数估算 Token）
     */
    private final int chunkSize;

    /**
     * 相邻块的重叠字符数
     */
    private final int chunkOverlap;

    /**
     * 分隔符优先级列表，按优先级从高到低排列
     * 例如：["\n\n", "\n", " "] 表示先按段落分，再按行分，最后按空格分
     */
    private final List<String> separators;

    /**
     * 是否保留分隔符在切块结果中
     */
    private final boolean keepSeparator;

    /**
     * 最小有效字符数，过短的块将被丢弃
     */
    private final int minChunkChars;

    /**
     * 构造器
     *
     * @param chunkSize     目标块大小（字符数）
     * @param chunkOverlap  重叠字符数
     * @param separators    分隔符优先级列表
     * @param keepSeparator 是否保留分隔符
     * @param minChunkChars 最小有效字符数
     */
    public ParentAwareTextSplitter(int chunkSize, int chunkOverlap, List<String> separators,
                                   boolean keepSeparator, int minChunkChars) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this
                .separators = separators != null ? separators : List
                .of("\n\n", "\n", " ");
        this.keepSeparator = keepSeparator;
        this.minChunkChars = minChunkChars;
    }

    /**
     * 执行文本分割
     * 两轮分割：第一轮按最高优先级分隔符分父块，第二轮对每个父块分子块
     *
     * @param text 原始文本
     * @return 分割后的文本块列表（包含父子关系）
     */
    public List<TextChunk> split(String text) {
        // 第一轮：按最高优先级分隔符（如段落）分割为父块
        List<TextChunk> parentChunks = splitBySeparator(text, separators.get(0), null);

        // 第二轮：对每个父块进行子块分割
        List<TextChunk> allChunks = new ArrayList<>();
        for (TextChunk parent : parentChunks) {
            List<TextChunk> children = splitChildren(parent);
            allChunks.addAll(children);
        }
        return allChunks;
    }

    /**
     * 按指定分隔符分割文本
     *
     * @param text      待分割文本
     * @param separator 分隔符
     * @param parentId  父块 ID（用于建立父子关系）
     * @return 分割后的文本块列表
     */
    private List<TextChunk> splitBySeparator(String text, String separator, String parentId) {
        List<TextChunk> chunks = new ArrayList<>();
        // 使用 Pattern.quote 转义分隔符中的特殊字符
        String quotedSeparator = Pattern.quote(separator);
        String[] parts = text.split(quotedSeparator);

        for (int i = 0; i < parts.length; i++) {
            // 跳过空白块
            if (parts[i].isBlank()) continue;

            // 根据配置决定是否保留分隔符（第一块不加前缀分隔符）
            String chunkText = keepSeparator && i > 0 ?
                    separator + parts[i] : parts[i];
            TextChunk chunk = new TextChunk(chunkText.trim());

            // 设置父块 ID，建立父子关系
            if (parentId != null) chunk.setParentId(parentId);
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * 对父块进行子块分割
     * 策略：优先使用次级分隔符，超长块按字符长度分割
     *
     * @param parent 父文本块
     * @return 子文本块列表
     */
    private List<TextChunk> splitChildren(TextChunk parent) {
        // 如果有次级分隔符，优先使用次级分隔符分割
        if (separators.size() > 1) {
            List<TextChunk> chunks = splitBySeparator(
                    parent.getText(),
                    separators.get(1),
                    parent.getId()
            );

            // 检查子块是否超长，需要进一步按长度分割
            List<TextChunk> result = new ArrayList<>();
            for (TextChunk chunk : chunks) {
                // 超过 4 倍 chunkSize 的块需要进一步分割
                if (chunk
                        .getText()
                        .length() > chunkSize * 4) {
                    splitByLength(chunk, result);
                } else {
                    result.add(chunk);
                }
            }
            return result;
        }
        // 无次级分隔符则直接按长度分割
        else {
            List<TextChunk> chunks = new ArrayList<>();
            splitByLength(parent, chunks);
            return chunks;
        }
    }

    /**
     * 按字符长度分割超长文本块
     * 使用滑动窗口，窗口大小为 chunkSize*4，步进为 (chunkSize-chunkOverlap)*4
     *
     * @param chunk  待分割的文本块
     * @param output 输出列表
     */
    private void splitByLength(TextChunk chunk, List<TextChunk> output) {
        String text = chunk.getText();
        int start = 0;

        while (start < text.length()) {
            // 计算窗口结束位置
            int maxEnd = start + chunkSize * 4;
            int textLength = text.length();
            int end = Math.min(maxEnd, textLength);
            String subText = text
                    .substring(start, end)
                    .trim();

            // 只保留达到最小长度要求的块
            if (subText.length() >= minChunkChars) {
                TextChunk subChunk = new TextChunk(subText);
                // 继承父块 ID 和元数据
                String parentId = chunk.getParentId();
                subChunk.setParentId(parentId);
                Map<String, Object> parentMetadata = chunk.getMetadata();
                subChunk
                        .getMetadata()
                        .putAll(parentMetadata);
                output.add(subChunk);
            }

            // 滑动窗口前进，保留重叠部分
            start = end - chunkOverlap * 4;
        }
    }
}
