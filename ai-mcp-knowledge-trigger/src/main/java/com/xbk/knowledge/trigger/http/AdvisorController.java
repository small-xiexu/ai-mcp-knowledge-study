package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IAdvisorService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.advisor.*;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.application.service.app.AdvisorAppService;
import com.xbk.knowledge.application.service.app.AdvisorBindingAppService;
import com.xbk.knowledge.domain.advisor.model.entity.Advisor;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingQuery;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorBindingView;
import com.xbk.knowledge.domain.advisor.model.valobj.AdvisorPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Advisor 资产与绑定管理接口。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/advisors")
@RequiredArgsConstructor
public class AdvisorController implements IAdvisorService {

    private final AdvisorAppService advisorAppService;
    private final AdvisorBindingAppService advisorBindingAppService;

    /**
     * list。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/list")
    @SaCheckPermission("advisor:read")
    @Override
    public Result<PageResult<AdvisorResponse>> list(@Valid @RequestBody AdvisorQueryRequest request) {
        Integer enabled = request.getEnabled() == null ? null : (request.getEnabled() ? 1 : 0);
        AdvisorPageQuery query = new AdvisorPageQuery(request.getKeyword(),
                enabled,
                request.getAdvisorType(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<Advisor> page = advisorAppService.queryPage(query);
        PageResult<AdvisorResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * get。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/get")
    @SaCheckPermission("advisor:read")
    @Override
    public Result<AdvisorResponse> get(@Valid @RequestBody IdRequest request) {
        Advisor advisor = advisorAppService.get(request.getId());
        return Result.success(toResponse(advisor));
    }

    /**
     * save。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/save")
    @SaCheckPermission("advisor:write")
    @Override
    public Result<AdvisorResponse> save(@Valid @RequestBody AdvisorSaveRequest request) {
        Advisor advisor = Advisor.builder()
                .id(request.getId())
                                .advisorCode(request.getAdvisorCode())
                .advisorName(request.getAdvisorName())
                .advisorType(request.getAdvisorType())
                .enabled(request.getEnabled() == null || request.getEnabled() ? 1 : 0)
                .configJson(request.getConfigJson())
                .build();
        Advisor saved = advisorAppService.save(advisor);
        return Result.success("保存成功", toResponse(saved));
    }

    /**
     * enable。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/enable")
    @SaCheckPermission("advisor:write")
    @Override
    public Result<AdvisorResponse> enable(@Valid @RequestBody IdRequest request) {
        Advisor enabled = advisorAppService.enable(request.getId());
        return Result.success(toResponse(enabled));
    }

    /**
     * disable。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/disable")
    @SaCheckPermission("advisor:write")
    @Override
    public Result<AdvisorResponse> disable(@Valid @RequestBody IdRequest request) {
        Advisor disabled = advisorAppService.disable(request.getId());
        return Result.success(toResponse(disabled));
    }

    /**
     * remove。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/remove")
    @SaCheckPermission("advisor:write")
    @Override
    public Result<Void> remove(@Valid @RequestBody IdRequest request) {
        advisorAppService.remove(request.getId());
        return Result.success();
    }

    /**
     * listBindings。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/bindings/list")
    @SaCheckPermission("advisor:read")
    @Override
    public Result<List<AdvisorBindingViewResponse>> listBindings(@Valid @RequestBody AdvisorBindingGetRequest request) {
        AdvisorBindingQuery query = new AdvisorBindingQuery(request.getBindType(), request.getBindTargetId());
        List<AdvisorBindingView> list = advisorBindingAppService.listBindings(query);
        if (CollectionUtils.isEmpty(list)) {
            return Result.success(List.of());
        }
        List<AdvisorBindingViewResponse> resp = new ArrayList<>();
        for (AdvisorBindingView v : list) {
            resp.add(toViewResponse(v));
        }
        return Result.success(resp);
    }

    /**
     * saveBindings。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/bindings/save")
    @SaCheckPermission("advisor:write")
    @Override
    public Result<Void> saveBindings(@Valid @RequestBody AdvisorBindingSaveRequest request) {
        List<AdvisorBindingAppService.AdvisorBindingSaveItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (AdvisorBindingSaveRequest.AdvisorBindingSaveItem it : request.getItems()) {
                if (it == null) {
                    continue;
                }
                AdvisorBindingAppService.AdvisorBindingSaveItem x = new AdvisorBindingAppService.AdvisorBindingSaveItem();
                x.setAdvisorId(it.getAdvisorId());
                x.setOrderNo(it.getOrderNo());
                x.setEnabled(it.getEnabled());
                items.add(x);
            }
        }
        advisorBindingAppService.saveBindings(request.getBindType(), request.getBindTargetId(), items);
        return Result.success("保存成功", null);
    }

    private AdvisorResponse toResponse(Advisor a) {
        if (a == null) {
            return null;
        }
        AdvisorResponse resp = new AdvisorResponse();
        resp.setId(a.getId());
        resp.setAdvisorCode(a.getAdvisorCode());
        resp.setAdvisorName(a.getAdvisorName());
        resp.setAdvisorType(a.getAdvisorType());
        resp.setEnabled(a.getEnabled());
        resp.setConfigJson(a.getConfigJson());
        resp.setCreatedAt(a.getCreatedAt());
        resp.setUpdatedAt(a.getUpdatedAt());
        return resp;
    }

    private AdvisorBindingViewResponse toViewResponse(AdvisorBindingView v) {
        if (v == null) {
            return null;
        }
        AdvisorBindingViewResponse resp = new AdvisorBindingViewResponse();
        resp.setBindingId(v.getBindingId());
        resp.setBindType(v.getBindType());
        resp.setBindTargetId(v.getBindTargetId());
        resp.setAdvisorId(v.getAdvisorId());
        resp.setOrderNo(v.getOrderNo());
        resp.setBindingEnabled(v.getBindingEnabled());
        resp.setBindingCreatedAt(v.getBindingCreatedAt());
        resp.setBindingUpdatedAt(v.getBindingUpdatedAt());
        resp.setAdvisorCode(v.getAdvisorCode());
        resp.setAdvisorName(v.getAdvisorName());
        resp.setAdvisorType(v.getAdvisorType());
        resp.setAdvisorEnabled(v.getAdvisorEnabled());
        resp.setAdvisorConfigJson(v.getAdvisorConfigJson());
        resp.setAdvisorCreatedAt(v.getAdvisorCreatedAt());
        resp.setAdvisorUpdatedAt(v.getAdvisorUpdatedAt());
        return resp;
    }

}
