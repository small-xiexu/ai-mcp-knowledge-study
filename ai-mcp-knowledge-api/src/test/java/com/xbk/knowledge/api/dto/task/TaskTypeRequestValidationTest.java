package com.xbk.knowledge.api.dto.task;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务类型请求的必填字段约束，避免任务配置缺失。
 *
 * @author xiexu
 */
public class TaskTypeRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 对外暴露 shouldRejectMissingRequiredFields 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectMissingRequiredFields() {
        TaskTypeRequest request = new TaskTypeRequest();

        Set<ConstraintViolation<TaskTypeRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "taskName".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "taskCode".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "preferredModelId".equals(v.getPropertyPath().toString())));
    }
}
