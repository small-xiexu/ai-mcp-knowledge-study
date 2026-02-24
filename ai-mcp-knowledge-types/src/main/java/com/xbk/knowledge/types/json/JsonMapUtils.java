package com.xbk.knowledge.types.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JSON Map 转换工具。
 *
 * 职责：统一 Map<String, Object> 的读取与转换逻辑，避免重复 TypeReference 与未检查强转。
 *
 * @author sxie
 */
public final class JsonMapUtils {

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<Map<String, Object>>() {};
    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE =
            new TypeReference<Map<String, String>>() {};

    /**
     * 创建 JSON 映射工具并注入依赖组件。
     */
    private JsonMapUtils() {
    }

    /**
     * 将 JSON 文本读取为可变 Map。
     *
     * @param objectMapper Jackson 对象
     * @param json JSON 文本
     * @return 可变 Map
     * @throws IOException JSON 解析异常
     */
    public static Map<String, Object> readMap(ObjectMapper objectMapper, String json) throws IOException {
        Objects.requireNonNull(objectMapper, "objectMapper 不能为空");
        Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    /**
     * 将任意对象转换为可变 Map。
     *
     * @param objectMapper Jackson 对象
     * @param source 源对象
     * @return 可变 Map
     */
    public static Map<String, Object> convertToMap(ObjectMapper objectMapper, Object source) {
        Objects.requireNonNull(objectMapper, "objectMapper 不能为空");
        if (source == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = objectMapper.convertValue(source, MAP_TYPE);
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    /**
     * 将 JSON 文本读取为字符串键值 Map。
     *
     * @param objectMapper Jackson 对象
     * @param json JSON 文本
     * @return 可变字符串 Map
     * @throws IOException JSON 解析异常
     */
    public static Map<String, String> readStringMap(ObjectMapper objectMapper, String json) throws IOException {
        Objects.requireNonNull(objectMapper, "objectMapper 不能为空");
        Map<String, String> map = objectMapper.readValue(json, STRING_MAP_TYPE);
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }
}
