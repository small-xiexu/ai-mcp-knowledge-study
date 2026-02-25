package com.xbk.knowledge.types.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证异常消息解析逻辑，避免错误提示被空值覆盖。
 *
 * @author xiexu
 */
public class ExceptionMessageUtilsTest {

    /**
     * 对外暴露 shouldUseExceptionMessageWhenPresent 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldUseExceptionMessageWhenPresent() {
        RuntimeException exception = new RuntimeException("boom");

        String message = ExceptionMessageUtils.resolveMessage(exception, "default", true);

        assertEquals("boom", message);
    }

    /**
     * 对外暴露 shouldAppendTypeWhenMessageMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldAppendTypeWhenMessageMissing() {
        RuntimeException exception = new RuntimeException(" ");

        String message = ExceptionMessageUtils.resolveMessage(exception, "默认错误", true);

        assertEquals("默认错误RuntimeException", message);
    }
}
