package com.xbk.knowledge.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 话创建请求
 *
 * @author sxie
 */
@Data
public class ChatSessionCreateRequest {

    /**
     * 话标题
     */
    @NotBlank(message = "title 不能为空")
    private String title;

    /**
     * 话默认模型ID
     */
    private Long modelId;

    /**
     * 关联知识库标签
     */
    private List<String> ragTags;

}
