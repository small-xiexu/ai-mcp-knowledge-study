package com.xbk.knowledge.api.dto.model;

import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证模型配置请求的必填约束，避免无效配置进入应用层。
 *
 * @author xiexu
 */
public class ModelConfigRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 对外暴露 shouldRejectMissingRequiredFields 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectMissingRequiredFields() {
        ModelConfigRequest request = new ModelConfigRequest();

        Set<ConstraintViolation<ModelConfigRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "modelName".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "modelType".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "apiKey".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "baseUrl".equals(v.getPropertyPath().toString())));
    }

    /**
     * 对外暴露 shouldApplyDefaultValuesWhenBuilding 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldApplyDefaultValuesWhenBuilding() {
        ModelConfigRequest request = ModelConfigRequest.builder()
                .modelName("gpt-test")
                .modelType(ModelType.OPENAI)
                .apiKey("test-key")
                .baseUrl("http://localhost")
                .build();

        assertEquals(Boolean.TRUE, request.getEnabled());
        assertEquals(Integer.valueOf(0), request.getPriority());
    }
}
