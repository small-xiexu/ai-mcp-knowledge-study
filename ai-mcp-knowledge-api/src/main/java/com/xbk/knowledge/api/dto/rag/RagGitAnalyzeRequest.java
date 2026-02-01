package com.xbk.knowledge.api.dto.rag;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * Git 仓库分析请求
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author xiexu
 */
@Data
public class RagGitAnalyzeRequest {

    /**
     * 仓库地址
     */
    @NotBlank(message = "仓库地址不能为空")
    private String repoUrl;

    /**
     * 用户名（可选）
     */
    private String userName;

    /**
     * 访问令牌（可选）
     */
    private String token;

    /**
     * 知识库标签（可选，不传则使用仓库名称）
     */
    private String ragTag;
}
