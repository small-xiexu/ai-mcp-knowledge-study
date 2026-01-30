package com.xbk.knowledge.trigger.exception;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.common.ResultCode;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 验证全局异常处理器的错误码映射，避免错误响应不一致。
 *
 * @author xiexu
 */
public class GlobalExceptionHandlerTest {

    /**
     * 对外暴露 shouldHandleBusinessException 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldHandleBusinessException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        BusinessException exception = new BusinessException(ResultCode.BAD_REQUEST.getCode(), "bad");
        Result<Object> result = handler.handleBusinessException(exception, request);

        assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
        assertEquals("bad", result.getMessage());
    }

    /**
     * 对外暴露 shouldHandleNotFoundException 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldHandleNotFoundException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        NotFoundException exception = new NotFoundException("missing");
        Result<Object> result = handler.handleNotFoundException(exception, request);

        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
        assertEquals("missing", result.getMessage());
    }

    /**
     * 对外暴露 shouldHandleIllegalArgumentException 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldHandleIllegalArgumentException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        Result<Object> result = handler.handleIllegalArgumentException(new IllegalArgumentException("bad"), request);

        assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
        assertEquals("bad", result.getMessage());
    }
}
