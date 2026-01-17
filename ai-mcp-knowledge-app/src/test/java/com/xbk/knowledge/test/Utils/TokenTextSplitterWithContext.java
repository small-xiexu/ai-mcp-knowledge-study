package com.xbk.knowledge.test.Utils;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于结巴分词的文本分割器（带上下文重叠）
 * 专为中文文本设计，使用结巴分词进行分词后按 Token 数切块
 * 支持 chunk 重叠，确保上下文连贯性，提升 RAG 检索效果
 *
 * @author xiexu
 */
public class TokenTextSplitterWithContext {

    /**
     * 每个文本块的目标 Token 数量
     */
    private final int chunkSize;

    /**
     * 相邻文本块的重叠 Token 数量
     * 重叠部分保留上下文，避免语义被截断
     */
    private final int chunkOverlap;

    /**
     * 构造器
     *
     * @param chunkSize    每个文本块的 Token 数量
     * @param chunkOverlap 相邻块之间的重叠 Token 数量
     */
    public TokenTextSplitterWithContext(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * 将文档列表分割为更小的文本块
     * 流程：遍历文档 -> 结巴分词 -> 滑动窗口切块 -> 继承原文档元数据
     *
     * @param documents 原始文档列表
     * @return 分割后的文档块列表
     */
    public List<Document> split(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            // 1. 对文档文本进行结巴分词
            String[] tokens = tokenize(doc.getText());
            int start = 0;

            // 2. 滑动窗口切块：每次取 chunkSize 个 Token，步进 (chunkSize - chunkOverlap)
            while (start < tokens.length) {
                int end = Math.min(start + chunkSize, tokens.length);

                // 3. 将 Token 数组拼接为文本字符串
                StringBuilder chunkBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    chunkBuilder.append(tokens[i]).append(" ");
                }
                String chunkText = chunkBuilder.toString().trim();

                // 4. 创建新文档并继承原文档的元数据
                Document chunkDoc = new Document(chunkText);
                chunkDoc.getMetadata().putAll(doc.getMetadata());
                result.add(chunkDoc);

                // 5. 滑动窗口前进，保留 overlap 部分
                start += (chunkSize - chunkOverlap);
            }
        }
        return result;
    }

    /**
     * 使用结巴分词对文本进行分词
     * 采用 INDEX 模式，适合搜索引擎场景
     *
     * @param text 原始文本
     * @return 分词后的 Token 数组
     */
    private String[] tokenize(String text) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        // INDEX 模式：在精确模式基础上，对长词再次切分，提高召回率
        List<SegToken> segTokens = segmenter.process(text, JiebaSegmenter.SegMode.INDEX);
        return segTokens.stream().map(token -> token.word).toArray(String[]::new);
    }

}
