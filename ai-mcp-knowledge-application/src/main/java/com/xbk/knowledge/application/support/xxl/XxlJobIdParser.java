package com.xbk.knowledge.application.support.xxl;

import org.springframework.util.StringUtils;

/**
 * XXL-Admin 创建任务返回结果解析器。
 *
 * 说明：当前 {@code /jobinfo/add} 的 content 预期为纯数字 jobId；
 * 为防止误绑错误 jobId，禁止从非数字字符串中“提取数字兜底”。
 *
 * @author sxie
 */
public final class XxlJobIdParser {

    private XxlJobIdParser() {
    }

    /**
     * 解析业务数据。
     *
     * @param content 输入内容
     * @return 处理后的结果
     */
    public static Long parseJobIdOrNull(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String t = content.trim();
        if (!t.matches("^\\d+$")) {
            return null;
        }
        try {
            return Long.parseLong(t);
        } catch (Exception e) {
            return null;
        }
    }
}

