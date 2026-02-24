package com.xbk.knowledge.application.support.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent 运行时 RAG 治理支持。
 *
 * 职责：
 * 1) 按 AgentVersion.rag_mode/default/allowed 对请求 ragTags 做规范化与白名单过滤
 * 2) 在需要时执行向量检索，返回文档与平台可控的 citations
 *
 * 说明：
 * - allowedRagTagsJson 为空：视为“不限”（允许任意 tag），避免误伤存量；若需要严格白名单，请显式配置 allowed
 * - REQUIRED 未命中：由上层短路返回（SUCCESS + uncertainty），避免模型“装懂”
 *
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRagGovernanceSupport {

    private static final int MAX_DOCS = 5;
    private static final int MAX_SNIPPET = 220;
    private static final int MAX_DOC_TEXT = 1600;

    private final ObjectMapper objectMapper;
    private final RagVectorStoreService ragVectorStoreService;

    /**
     * 解析并执行 RAG（按 AgentVersion 策略）。
     *
     * @param version        AgentVersion（已发布）
     * @param ragTagsJson    请求侧 ragTagsJson（可空）
     * @param query          查询文本（一般为用户输入）
     * @return 解析结果（永不为 null）
     */
    public ResolvedRag resolve(AgentVersion version, String ragTagsJson, String query) {
        String mode = version == null ? null : version.getRagMode();
        if (!StringUtils.hasText(mode)) {
            mode = "OPTIONAL";
        }
        String normalizedMode = mode.trim().toUpperCase();

        List<String> requested = parseStringListOrEmpty(ragTagsJson);
        List<String> defaults = parseStringListOrEmpty(version == null ? null : version.getDefaultRagTagsJson());
        List<String> allowed = parseStringListOrEmpty(version == null ? null : version.getAllowedRagTagsJson());

        List<String> base = !requested.isEmpty() ? requested : defaults;

        if ("DISABLED".equals(normalizedMode)) {
            return ResolvedRag.builder()
                    .mode(normalizedMode)
                    .requestedTags(requested)
                    .defaultTags(defaults)
                    .allowedTags(allowed)
                    .effectiveTags(List.of())
                    .droppedTags(List.of())
                    .documents(List.of())
                    .citations(List.of())
                    .required(false)
                    .requiredMiss(false)
                    .build();
        }

        FilteredTags filtered = filterByAllowList(base, allowed);
        List<String> effective = filtered.effective;
        boolean required = "REQUIRED".equals(normalizedMode);

        if (effective.isEmpty()) {
            return ResolvedRag.builder()
                    .mode(normalizedMode)
                    .requestedTags(requested)
                    .defaultTags(defaults)
                    .allowedTags(allowed)
                    .effectiveTags(effective)
                    .droppedTags(filtered.dropped)
                    .documents(List.of())
                    .citations(List.of())
                    .required(required)
                    .requiredMiss(required)
                    .missReason(required ? "REQUIRED 模式下未提供可用的 RAG tags（请求 tags 为空或被白名单剔除）" : null)
                    .build();
        }

        if (!StringUtils.hasText(query)) {
            return ResolvedRag.builder()
                    .mode(normalizedMode)
                    .requestedTags(requested)
                    .defaultTags(defaults)
                    .allowedTags(allowed)
                    .effectiveTags(effective)
                    .droppedTags(filtered.dropped)
                    .documents(List.of())
                    .citations(List.of())
                    .required(required)
                    .requiredMiss(required)
                    .missReason(required ? "REQUIRED 模式下 query 为空，无法执行检索" : null)
                    .build();
        }

        List<Document> docs;
        try {
            docs = ragVectorStoreService.similaritySearch(query, effective);
        } catch (Exception e) {
            log.warn("RAG similaritySearch 执行失败，mode={}, tags={}", normalizedMode, effective, e);
            docs = List.of();
        }
        if (CollectionUtils.isEmpty(docs)) {
            return ResolvedRag.builder()
                    .mode(normalizedMode)
                    .requestedTags(requested)
                    .defaultTags(defaults)
                    .allowedTags(allowed)
                    .effectiveTags(effective)
                    .droppedTags(filtered.dropped)
                    .documents(List.of())
                    .citations(List.of())
                    .required(required)
                    .requiredMiss(required)
                    .missReason(required ? "REQUIRED 模式下检索未命中（documents 为空）" : null)
                    .build();
        }

        List<Document> topDocs = new ArrayList<>();
        for (Document d : docs) {
            if (d == null) {
                continue;
            }
            if (topDocs.size() >= MAX_DOCS) {
                break;
            }
            String text = d.getText();
            if (text != null && text.length() > MAX_DOC_TEXT) {
                text = text.substring(0, MAX_DOC_TEXT);
                topDocs.add(new Document(text, d.getMetadata()));
            } else {
                topDocs.add(d);
            }
        }

        List<PlatformContractV1.Citation> citations = buildCitations(topDocs);
        return ResolvedRag.builder()
                .mode(normalizedMode)
                .requestedTags(requested)
                .defaultTags(defaults)
                .allowedTags(allowed)
                .effectiveTags(effective)
                .droppedTags(filtered.dropped)
                .documents(topDocs)
                .citations(citations)
                .required(required)
                .requiredMiss(false)
                .build();
    }

    /**
     * 解析字符串列表（为空时返回空集合）。
     *
     * @param json JSON 字符串。
     * @return 返回解析后的列表结果。
     */
    private List<String> parseStringListOrEmpty(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (String s : list) {
                if (!StringUtils.hasText(s)) {
                    continue;
                }
                String t = s.trim();
                if (StringUtils.hasText(t)) {
                    set.add(t);
                }
            }
            return new ArrayList<>(set);
        } catch (Exception e) {
            log.warn("解析字符串数组 JSON 失败，json: {}", json, e);
            return List.of();
        }
    }

    /**
     * 按白名单过滤标签集合。
     *
     * @param base 基础标签集合。
     * @param allowed 允许标签集合。
     * @return 返回FilteredTags对象。
     */
    private FilteredTags filterByAllowList(List<String> base, List<String> allowed) {
        if (CollectionUtils.isEmpty(base)) {
            return new FilteredTags(List.of(), List.of());
        }
        if (CollectionUtils.isEmpty(allowed)) {
            // allowed 为空视为“不限”
            return new FilteredTags(base, List.of());
        }
        Set<String> allowSet = new LinkedHashSet<>(allowed);
        List<String> effective = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        for (String t : base) {
            if (!StringUtils.hasText(t)) {
                continue;
            }
            String tag = t.trim();
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            if (allowSet.contains(tag)) {
                effective.add(tag);
            } else {
                dropped.add(tag);
            }
        }
        return new FilteredTags(effective, dropped);
    }

    /**
     * 构建引用信息列表。
     *
     * @param docs 文档列表。
     * @return 返回构建结果对象。
     */
    private List<PlatformContractV1.Citation> buildCitations(List<Document> docs) {
        if (CollectionUtils.isEmpty(docs)) {
            return List.of();
        }
        List<PlatformContractV1.Citation> list = new ArrayList<>();
        for (Document d : docs) {
            if (d == null) {
                continue;
            }
            if (list.size() >= MAX_DOCS) {
                break;
            }
            Map<String, Object> md = d.getMetadata();
            String knowledge = md == null ? null : safeToString(md.get("knowledge"));
            String source = md == null ? null : safeToString(md.get("source"));
            String title = StringUtils.hasText(source) ? source : (StringUtils.hasText(knowledge) ? knowledge : "RAG");
            String src = StringUtils.hasText(knowledge) ? ("rag:" + knowledge) : "rag";
            String snippet = d.getText() == null ? "" : d.getText().trim();
            if (snippet.length() > MAX_SNIPPET) {
                snippet = snippet.substring(0, MAX_SNIPPET);
            }
            list.add(PlatformContractV1.Citation.builder()
                    .title(title)
                    .snippet(snippet)
                    .source(src)
                    .build());
        }
        return list;
    }

    /**
     * 将对象安全转换为非空白字符串。
     *
     * @param o 输入对象。
     * @return 返回非空白字符串，空白或空对象返回 null。
     */
    private String safeToString(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o);
        return StringUtils.hasText(s) ? s : null;
    }

    /**
     * 构建过滤后的标签结果对象。
     *
     * @param effective 生效标签集合。
     * @param dropped 未命中标签集合。
     * @return 返回记录对象。
     */
    private record FilteredTags(List<String> effective, List<String> dropped) {
    }

    /**
     * RAG 解析与检索结果。
     */
    @lombok.Builder
    public record ResolvedRag(
            String mode,
            List<String> requestedTags,
            List<String> defaultTags,
            List<String> allowedTags,
            List<String> effectiveTags,
            List<String> droppedTags,
            List<Document> documents,
            List<PlatformContractV1.Citation> citations,
            boolean required,
            boolean requiredMiss,
            String missReason
    ) {
    }
}
