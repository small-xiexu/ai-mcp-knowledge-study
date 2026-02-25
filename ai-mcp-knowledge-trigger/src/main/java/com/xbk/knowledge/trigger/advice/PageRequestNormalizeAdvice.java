package com.xbk.knowledge.trigger.advice;

import com.xbk.knowledge.types.common.PageRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 分页参数自动归一化
 * 在反序列化完成后自动修正分页参数
 * <p>
 * 职责：接口适配增强，用于统一请求参数修正
 *
 * @author sxie
 */
@RestControllerAdvice
public class PageRequestNormalizeAdvice implements RequestBodyAdvice {

    /**
     * 确定是否启用该 Advice
     * 统一拦截所有请求体以便后续做归一化处理
     * 
     * @param methodParameter 当前控制器方法参数信息。
     * @param targetType 目标请求体类型。
     * @param converterType 消息转换器类型。
     * @return `true` 表示启用该 Advice。
     */
    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 读取请求体之前的钩子
     * 此处无需改写输入流，直接透传
     * 
     * @param inputMessage 原始请求体输入流。
     * @param parameter 当前控制器方法参数信息。
     * @param targetType 目标请求体类型。
     * @param converterType 消息转换器类型。
     * @return 原始请求体输入流。
     */
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
                                           MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return inputMessage;
    }

    /**
     * 读取请求体之后的钩子
     * 对分页请求统一执行参数修正
     * <p>
     * 避免各接口重复校验分页边界，保证分页口径一致。
     * 
     * @param body 反序列化后的请求体。
     * @param inputMessage 原始请求体输入流。
     * @param parameter 当前控制器方法参数信息。
     * @param targetType 目标请求体类型。
     * @param converterType 消息转换器类型。
     * @return 归一化后的请求体。
     */
    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (body instanceof PageRequest) {
            PageRequest pageRequest = (PageRequest) body;
            pageRequest.validate();
        }
        return body;
    }

    /**
     * 请求体为空时的处理
     * 保持原样返回
     * 
     * @param body 空请求体对应的默认值。
     * @param inputMessage 原始请求体输入流。
     * @param parameter 当前控制器方法参数信息。
     * @param targetType 目标请求体类型。
     * @param converterType 消息转换器类型。
     * @return 原始空请求体值。
     */
    @Override
    public Object handleEmptyBody(Object body,
                                  HttpInputMessage inputMessage,
                                  MethodParameter parameter,
                                  Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }
}
