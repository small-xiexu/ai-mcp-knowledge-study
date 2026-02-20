package com.xbk.knowledge.application.service.selection.factory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.xbk.knowledge.application.service.selection.chain.ModelSelectionChain;
import com.xbk.knowledge.application.service.selection.handler.ModelSelectionHandler;
import java.util.Objects;
import java.util.Map;

/**
 * 模型选择责任链工厂
 * 通过枚举固定顺序组装责任链，链路结构在此集中呈现
 *
 * @author sxie
 */
@Component
public class ModelSelectionChainFactory {

    /**
     * 责任链头节点，负责驱动后续节点
     */
    private ModelSelectionHandler chainHead;

    /**
     * 1. 通过构造函数注入处理器集合。
     * 2. Spring 可自动注入 ModelSelectionHandler 实现到 map 中，key 为 bean 名称。
     * 3. 责任链顺序固定：显式策略 -> 默认兜底。
     * 4. 找到第一个责任链，然后将后续责任链依次挂载到第一个责任链上。
     *
     * @param handlerGroup Spring 注入的所有处理器
     */
    @Autowired
    public ModelSelectionChainFactory(Map<String, ModelSelectionHandler> handlerGroup) {
        ModelSelectionHandler head = Objects.requireNonNull(
                handlerGroup.get(ModelSelectionHandlerOrder.explicit_strategy.getBeanName()),
                "未找到责任链处理器：" + ModelSelectionHandlerOrder.explicit_strategy.getBeanName()
        );
        ModelSelectionHandler defaultHandler = Objects.requireNonNull(
                handlerGroup.get(ModelSelectionHandlerOrder.default_fallback.getBeanName()),
                "未找到责任链处理器：" + ModelSelectionHandlerOrder.default_fallback.getBeanName()
        );
        head.appendNext(defaultHandler);
        this.chainHead = head;
    }

    /**
     * 对外暴露责任链实例
     *
     * @return 模型选择责任链
     */
    public ModelSelectionChain openChain() {
        return new ModelSelectionChain(chainHead);
    }

    @Getter
    @AllArgsConstructor
    public enum ModelSelectionHandlerOrder {

        explicit_strategy("explicitStrategySelectionHandler", "显式策略处理器"),
        default_fallback("defaultSelectionHandler", "默认兜底处理器");

        private final String beanName;
        private final String info;

    }
}
