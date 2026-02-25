package com.xbk.knowledge.types.exception;

/**
 * 异常消息处理工具
 * 统一异常消息解析逻辑，避免各处重复拼装导致风格不一致
 *
 * 职责：异常信息处理工具，用于提升日志与错误响应的可读性
 * @author sxie
 */
public final class ExceptionMessageUtils {

    private ExceptionMessageUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 解析异常消息
     * 优先返回异常 message，避免空消息导致定位困难
     * 
     * @param throwable 需要解析的异常
     * @param defaultMessage 默认提示语
     * @param appendType 是否追加异常类型名称
     * @return 解析后的消息
     */
    public static String resolveMessage(Throwable throwable, String defaultMessage, boolean appendType) {
        if (throwable == null) {
            return defaultMessage;
        }
        String message = throwable.getMessage();
        String trimmedMessage = message == null ? null : message.trim();
        boolean hasMessage = trimmedMessage != null && !trimmedMessage.isEmpty();
        if (hasMessage) {
            return message;
        }
        Class<?> exceptionClass = throwable.getClass();
        String typeName = exceptionClass.getSimpleName();
        String trimmedTypeName = typeName == null ? null : typeName.trim();
        boolean hasTypeName = trimmedTypeName != null && !trimmedTypeName.isEmpty();
        String trimmedDefaultMessage = defaultMessage == null ? null : defaultMessage.trim();
        boolean hasDefaultMessage = trimmedDefaultMessage != null && !trimmedDefaultMessage.isEmpty();
        if (appendType && hasTypeName) {
            if (hasDefaultMessage) {
                return defaultMessage + "" + typeName;
            }
            return typeName;
        }
        if (hasDefaultMessage) {
            return defaultMessage;
        }
        return typeName;
    }
}
