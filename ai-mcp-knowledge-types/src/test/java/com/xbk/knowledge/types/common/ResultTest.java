package com.xbk.knowledge.types.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证统一返回结构的默认码值与数据装配行为，避免响应口径漂移。
 *
 * @author xiexu
 */
public class ResultTest {

    /**
     * 对外暴露 shouldBuildSuccessWithoutData 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldBuildSuccessWithoutData() {
        Result<String> result = Result.success();

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.getMessage());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp() > 0);
    }

    /**
     * 对外暴露 shouldBuildSuccessWithData 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldBuildSuccessWithData() {
        Result<String> result = Result.success("ok");

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.getMessage());
        assertEquals("ok", result.getData());
        assertNotNull(result.getTimestamp());
    }

    /**
     * 对外暴露 shouldBuildErrorWithMessage 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldBuildErrorWithMessage() {
        Result<String> result = Result.error("failed");

        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), result.getCode());
        assertEquals("failed", result.getMessage());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
    }

    /**
     * 对外暴露 shouldBuildErrorWithResultCodeAndMessage 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldBuildErrorWithResultCodeAndMessage() {
        Result<String> result = Result.error(ResultCode.BAD_REQUEST, "bad");

        assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
        assertEquals("bad", result.getMessage());
        assertNotNull(result.getTimestamp());
    }
}
