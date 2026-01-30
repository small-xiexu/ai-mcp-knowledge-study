package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.types.time.TimeCostUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 模型调用计时装饰器
 * 统一记录模型调用耗时，避免业务层重复计时代码
 *
 * @author xiexu
 */
@Primary
@Component
public class TimedModelCallExecutor implements ModelCallExecutor {

    private final ModelCallExecutor delegate;

    public TimedModelCallExecutor(@Qualifier("defaultModelCallExecutor") ModelCallExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public AICallResult execute(ModelCallContext context) {
        long startTime = TimeCostUtils.start();
        AICallResult result = delegate.execute(context);
        long costTime = TimeCostUtils.costMillis(startTime);
        if (result != null) {
            result.setResponseTime(costTime);
        }
        return result;
    }
}
