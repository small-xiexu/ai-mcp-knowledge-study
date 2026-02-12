package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolBinding;
import com.xbk.knowledge.domain.model.vo.gateway.ToolBindingQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工具绑定关系 Mapper
 *
 * @author xiexu
 */
@Mapper
public interface McpToolBindingMapper extends BaseMapper<McpToolBinding> {

    /** 新增工具绑定关系 */
    int insertToolBinding(McpToolBinding binding);

    /** 更新工具绑定关系 */
    int updateToolBinding(McpToolBinding binding);

    /** 按主键删除工具绑定关系 */
    int deleteToolBindingById(Long id);

    /** 按工具 ID 批量删除绑定关系（级联删除场景） */
    int deleteToolBindingByToolId(Long toolId);

    /** 按绑定类型（MODEL/SESSION）+ 目标 ID 查询绑定列表 */
    List<McpToolBinding> findByBindTypeAndTargetId(ToolBindingQuery query);

    /** 按工具 ID 查询该工具的所有绑定关系 */
    List<McpToolBinding> findByToolId(Long toolId);
}
