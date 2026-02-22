package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.infrastructure.dao.po.ClientProfileStepPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Client Profile Step DAO（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IClientProfileStepDao extends BaseMapper<ClientProfileStepPO> {

    List<ClientProfileStepPO> listByClientProfileId(@Param("clientProfileId") Long clientProfileId);

    int deleteByClientProfileId(@Param("clientProfileId") Long clientProfileId);

    int batchInsert(@Param("steps") List<ClientProfileStepPO> steps);
}
