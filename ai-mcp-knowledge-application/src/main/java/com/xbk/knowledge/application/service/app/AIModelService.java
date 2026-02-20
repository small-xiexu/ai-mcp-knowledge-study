package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;

/**
 * AI 模型统一服务接口
 * 提供统一的 AI 模型调用入口
 *
 * 职责：应用层用例接口，用于定义编排能力
 * @author xiexu
 */
public interface AIModelService {

    /**
     * 通用聊天接口
     * 根据请求中的策略自动选择模型
     *
     * @param request AI 请求对象
     * @return AI 响应对象
     */
    AICallResult chat(AICallCommand request);

}
