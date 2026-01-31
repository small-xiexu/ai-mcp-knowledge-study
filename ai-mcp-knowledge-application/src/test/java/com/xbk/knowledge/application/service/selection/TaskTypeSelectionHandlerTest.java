package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.service.selection.handler.TaskTypeSelectionHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务类型选择处理器的触发条件与返回值。
 *
 * @author xiexu
 */
public class TaskTypeSelectionHandlerTest {

    /**
     * 对外暴露 shouldSupportWhenTaskTypeProvided 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSupportWhenTaskTypeProvided() {
        TaskTypeSelectionHandler handler = new TaskTypeSelectionHandler();
        AICallCommand command = AICallCommand.builder().taskType("task").build();

        assertTrue(handler.supports(command));
        assertEquals("task", handler.select(command).getTaskType());
    }

    /**
     * 对外暴露 shouldNotSupportWhenTaskTypeMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldNotSupportWhenTaskTypeMissing() {
        TaskTypeSelectionHandler handler = new TaskTypeSelectionHandler();
        AICallCommand command = AICallCommand.builder().build();

        assertFalse(handler.supports(command));
    }
}
