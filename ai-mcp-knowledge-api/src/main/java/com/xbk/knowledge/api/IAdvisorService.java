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
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<AdvisorResponse>> list(AdvisorQueryRequest request);

    /**
     * 查询详情信息。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<AdvisorResponse> get(IdRequest request);

    /**
     * 保存配置信息。
     *
     * @param request 请求参数
     * @return 保存结果
     */
    Result<AdvisorResponse> save(AdvisorSaveRequest request);

    /**
     * 启用目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<AdvisorResponse> enable(IdRequest request);

    /**
     * 禁用目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<AdvisorResponse> disable(IdRequest request);

    /**
     * 删除目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> remove(IdRequest request);

    /**
     * 查询绑定关系列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<List<AdvisorBindingViewResponse>> listBindings(AdvisorBindingGetRequest request);

    /**
     * 保存绑定关系配置。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> saveBindings(AdvisorBindingSaveRequest request);
}
