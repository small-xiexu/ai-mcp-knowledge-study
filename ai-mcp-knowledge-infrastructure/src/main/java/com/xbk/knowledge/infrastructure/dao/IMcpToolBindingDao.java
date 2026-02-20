package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.McpToolBindingPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工具绑定关系 Mapper
 *
 * @author sxie
 */
@Mapper
public interface IMcpToolBindingDao extends BaseMapper<McpToolBindingPO> {

    /** 新增工具绑定关系 */
    int insertToolBinding(McpToolBindingPO binding);

    /** 更新工具绑定关系 */
    int updateToolBinding(McpToolBindingPO binding);

    /** 按主键删除工具绑定关系 */
    int deleteToolBindingById(IdQuery query);

    /** 按工具 ID 批量删除绑定关系（级联删除场景） */
    int deleteToolBindingByToolId(ToolIdQuery query);

    /** 按绑定类型（MODEL/SESSION）+ 目标 ID 查询绑定列表 */
    List<McpToolBindingPO> findByBindTypeAndTargetId(ToolBindingQuery query);

    /** 按工具 ID 查询该工具的所有绑定关系 */
    List<McpToolBindingPO> findByToolId(ToolIdQuery query);
}
