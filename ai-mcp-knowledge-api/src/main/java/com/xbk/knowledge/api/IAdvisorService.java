package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.advisor.AdvisorBindingGetRequest;
import com.xbk.knowledge.api.dto.advisor.AdvisorBindingSaveRequest;
import com.xbk.knowledge.api.dto.advisor.AdvisorBindingViewResponse;
import com.xbk.knowledge.api.dto.advisor.AdvisorQueryRequest;
import com.xbk.knowledge.api.dto.advisor.AdvisorResponse;
import com.xbk.knowledge.api.dto.advisor.AdvisorSaveRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;

/**
 * Advisor 管理服务接口
 * 定义 Advisor 配置与绑定管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAdvisorService {

    /**
     * 按筛选条件分页查询顾问策略数据。
     *
     * @param request 顾问策略查询条件。
     * @return 返回 AdvisorResponse 分页数据。
     */
    Result<PageResult<AdvisorResponse>> list(AdvisorQueryRequest request);

    /**
     * 查询顾问策略详情。
     *
     * @param request 顾问策略详情查询参数。
     * @return 返回 AdvisorResponse 数据。
     */
    Result<AdvisorResponse> get(IdRequest request);

    /**
     * 创建或更新顾问策略。
     *
     * @param request 顾问策略保存参数。
     * @return 返回 AdvisorResponse 数据。
     */
    Result<AdvisorResponse> save(AdvisorSaveRequest request);

    /**
     * 启用顾问策略。
     *
     * @param request 顾问策略启用参数。
     * @return 启用结果
     */
    Result<AdvisorResponse> enable(IdRequest request);

    /**
     * 禁用顾问策略。
     *
     * @param request 顾问策略禁用参数。
     * @return 禁用结果
     */
    Result<AdvisorResponse> disable(IdRequest request);

    /**
     * 删除顾问策略。
     *
     * @param request 顾问策略删除参数。
     * @return 返回顾问策略删除状态。
     */
    Result<Void> remove(IdRequest request);

    /**
     * 查询绑定关系列表。
     *
     * @param request 绑定关系查询参数。
     * @return 返回 AdvisorBindingViewResponse 列表数据。
     */
    Result<List<AdvisorBindingViewResponse>> listBindings(AdvisorBindingGetRequest request);

    /**
     * 保存绑定关系配置。
     *
     * @param request 绑定关系保存参数。
     * @return 返回绑定关系保存状态。
     */
    Result<Void> saveBindings(AdvisorBindingSaveRequest request);
}
