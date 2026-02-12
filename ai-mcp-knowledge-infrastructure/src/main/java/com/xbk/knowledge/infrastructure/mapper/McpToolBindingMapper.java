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

    int insertToolBinding(McpToolBinding binding);

    int updateToolBinding(McpToolBinding binding);

    int deleteToolBindingById(Long id);

    int deleteToolBindingByToolId(Long toolId);

    List<McpToolBinding> findByBindTypeAndTargetId(ToolBindingQuery query);

    List<McpToolBinding> findByToolId(Long toolId);
}
