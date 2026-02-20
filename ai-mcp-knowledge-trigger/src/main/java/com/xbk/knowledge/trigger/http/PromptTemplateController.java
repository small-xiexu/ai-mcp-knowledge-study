package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.agent.PromptTemplateCreateRequest;
import com.xbk.knowledge.api.dto.agent.PromptTemplatePublishRequest;
import com.xbk.knowledge.api.dto.agent.PromptTemplateQueryRequest;
import com.xbk.knowledge.api.dto.agent.PromptTemplateResponse;
import com.xbk.knowledge.api.dto.agent.PromptTemplateUpdateRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.xbk.knowledge.application.service.app.PromptTemplateAppService;
import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * PromptTemplate 控制面接口。
 *
 * 职责：HTTP 接口适配，用于转发应用层能力（模板创建/编辑/发布/归档/查询）。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateAppService promptTemplateAppService;
    private final IdentityContextService identityContextService;

    /**
     * 分页查询模板列表。
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    public Result<PageResult<PromptTemplateResponse>> list(@Valid @RequestBody PromptTemplateQueryRequest request) {
        PromptTemplatePageQuery query = new PromptTemplatePageQuery(request.getKeyword(),
                request.getState(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<PromptTemplate> page = promptTemplateAppService.queryPage(query);
        PageResult<PromptTemplateResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 根据模板 ID 查询模板详情。
     *
     * @param request 模板 ID 请求
     * @return 模板详情
     */
    @PostMapping("/get")
    @SaCheckPermission("agent:read")
    public Result<PromptTemplateResponse> get(@Valid @RequestBody IdRequest request) {
        PromptTemplate template = promptTemplateAppService.queryById(new PromptTemplateIdQuery(request.getId()));
        return Result.success(toResponse(template));
    }

    /**
     * 创建模板。
     *
     * @param request 创建请求
     * @return 创建后的模板
     */
    @PostMapping("/create")
    @SaCheckPermission("agent:write")
    public Result<PromptTemplateResponse> create(@Valid @RequestBody PromptTemplateCreateRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        PromptTemplate template = PromptTemplate.builder()
                .templateCode(request.getTemplateCode())
                .templateName(request.getTemplateName())
                .content(request.getContent())
                .variableSpecJson(request.getVariableSpecJson())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        PromptTemplate saved = promptTemplateAppService.create(template);
        return Result.success("模板创建成功", toResponse(saved));
    }

    /**
     * 更新模板草稿（仅 DRAFT 可更新）。
     *
     * @param request 更新请求
     * @return 更新后的模板
     */
    @PostMapping("/update")
    @SaCheckPermission("agent:write")
    public Result<PromptTemplateResponse> update(@Valid @RequestBody PromptTemplateUpdateRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        PromptTemplate template = PromptTemplate.builder()
                .id(request.getId())
                .templateName(request.getTemplateName())
                .content(request.getContent())
                .variableSpecJson(request.getVariableSpecJson())
                .updatedBy(userId)
                .build();
        PromptTemplate updated = promptTemplateAppService.updateDraft(template);
        return Result.success("模板更新成功", toResponse(updated));
    }

    /**
     * 发布模板（version_no 自增）。
     *
     * @param request 发布请求
     * @return 发布后的模板
     */
    @PostMapping("/publish")
    @SaCheckPermission("agent:publish")
    public Result<PromptTemplateResponse> publish(@Valid @RequestBody PromptTemplatePublishRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        PromptTemplate published = promptTemplateAppService.publish(new PromptTemplateIdQuery(request.getId()), userId);
        return Result.success("模板发布成功", toResponse(published));
    }

    /**
     * 归档模板。
     *
     * @param request 归档请求
     * @return 归档后的模板
     */
    @PostMapping("/archive")
    @SaCheckPermission("agent:publish")
    public Result<PromptTemplateResponse> archive(@Valid @RequestBody PromptTemplatePublishRequest request) {
        Long userId = identityContextService.getCurrentUserId();
        PromptTemplate archived = promptTemplateAppService.archive(new PromptTemplateIdQuery(request.getId()), userId);
        return Result.success("模板归档成功", toResponse(archived));
    }

    /**
     * 领域对象转输出 DTO。
     *
     * @param t PromptTemplate 实体
     * @return 响应 DTO
     */
    private PromptTemplateResponse toResponse(PromptTemplate t) {
        return PromptTemplateResponse.builder()
                .id(t.getId())
                .templateCode(t.getTemplateCode())
                .templateName(t.getTemplateName())
                .versionNo(t.getVersionNo())
                .state(t.getState())
                .content(t.getContent())
                .variableSpecJson(t.getVariableSpecJson())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

}
