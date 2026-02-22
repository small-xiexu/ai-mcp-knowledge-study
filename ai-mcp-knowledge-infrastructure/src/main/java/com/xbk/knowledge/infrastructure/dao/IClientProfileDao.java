package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.client.model.valobj.ClientProfilePageQuery;
import com.xbk.knowledge.infrastructure.dao.po.ClientProfilePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Client Profile DAO（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IClientProfileDao extends BaseMapper<ClientProfilePO> {

    ClientProfilePO findById(@Param("id") Long id);

    ClientProfilePO findByCode(@Param("clientCode") String clientCode);

    List<ClientProfilePO> findPage(ClientProfilePageQuery query);

    long count(ClientProfilePageQuery query);

    int insertClientProfile(ClientProfilePO profile);

    int updateClientProfile(ClientProfilePO profile);

    int deleteById(@Param("id") Long id);
}
