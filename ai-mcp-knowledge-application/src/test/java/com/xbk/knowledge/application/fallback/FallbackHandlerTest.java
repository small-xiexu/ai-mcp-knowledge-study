package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;

/**
 * 验证降级处理器的执行转发，确保失败时可触发降级流程。
 *
 * @author xiexu
 */
public class FallbackHandlerTest {

    /**
     * 对外暴露 shouldDelegateToFailoverExecutor 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDelegateToFailoverExecutor() {
        FailoverExecutor executor = Mockito.mock(FailoverExecutor.class);
        FallbackHandler handler = new FallbackHandler(executor);

        handler.executeWithFallback(ModelConfig.builder().build(), Collections.<ModelConfig>emptyList(), AICallCommand.builder().build());

        verify(executor).execute(Mockito.any(ModelConfig.class), Mockito.anyList(), Mockito.any(AICallCommand.class));
    }
}
