package com.xbk.knowledge.domain.service.mcp.impl;

import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerNameQuery;
import com.xbk.knowledge.domain.repository.mcp.McpServerConfigRepository;
import com.xbk.knowledge.domain.service.mcp.IMcpServerConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * MCP Server 配置领域服务实现
 * 封装 MCP Server 配置的业务逻辑
 *
 * 职责：领域服务实现，用于封装业务规则
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerConfigServiceImpl implements IMcpServerConfigService {

    private final McpServerConfigRepository mcpServerConfigRepository;

    /**
     * 分页查询 MCP Server 配置
     *
     * 为什么：统一分页口径，避免前端传参与仓储不一致
     * 入参：分页查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<McpServerConfig> queryMcpServerConfigPage(McpServerConfigPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        /*
         * 目的：规范化分页参数，避免异常分页导致性能问题
         */
        McpServerConfigPageQuery pageQuery = new McpServerConfigPageQuery(offset, pageSize);
        List<McpServerConfig> configs = mcpServerConfigRepository.findPage(pageQuery);

        long total = mcpServerConfigRepository.countAll();
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(configs, total, pageNum, pageSize);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 为什么：不存在时抛出领域异常，避免空对象传播
     * 入参：ID 查询对象
     * 出参：配置详情
     */
    @Override
    public McpServerConfig queryMcpServerConfigById(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("MCP Server ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "MCP Server 配置不存在，id: " + id;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        return mcpServerConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);
    }

    /**
     * 创建 MCP Server 配置
     *
     * 为什么：创建时确保名称唯一，避免重复配置
     * 入参：配置实体
     * 出参：创建后的配置
     */
    @Override
    public McpServerConfig createMcpServerConfig(McpServerConfig config) {
        /*
         * 目的：校验名称唯一性，避免数据库异常
         */
        String serverName = config.getServerName();
        McpServerNameQuery nameQuery = new McpServerNameQuery(serverName);
        if (mcpServerConfigRepository
                .findByName(nameQuery)
                .isPresent()) {
            throw new IllegalArgumentException("MCP Server 名称已存在：" + serverName);
        }

        /*
         * 目的：补齐创建/更新时间，保证审计字段一致
         */
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        return mcpServerConfigRepository.save(config);
    }

    /**
     * 更新 MCP Server 配置
     *
     * 为什么：更新前校验唯一性与存在性，避免配置冲突
     * 入参：配置实体
     * 出参：更新后的配置
     */
    @Override
    public McpServerConfig updateMcpServerConfig(McpServerConfig config) {
        if (config.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供 MCP Server ID");
        }

        /*
         * 目的：读取现有配置，确保更新基于最新数据
         */
        Long configId = config.getId();
        IdQuery idQuery = new IdQuery(configId);
        String notFoundMessage = "MCP Server 配置不存在，id: " + configId;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        McpServerConfig existingConfig = mcpServerConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        /*
         * 目的：校验名称唯一性，避免冲突
         */
        String serverName = config.getServerName();
        McpServerNameQuery nameQuery = new McpServerNameQuery(serverName);
        mcpServerConfigRepository
                .findByName(nameQuery)
                .ifPresent(existing -> {
                    if (!existing
                            .getId()
                            .equals(configId)) {
                        throw new IllegalArgumentException("MCP Server 名称已存在：" + serverName);
                    }
                });

        /*
         * 目的：覆盖可更新字段并刷新更新时间
         */
        existingConfig.setServerName(serverName);
        existingConfig.setServerType(config.getServerType());
        existingConfig.setEnabled(config.getEnabled());
        existingConfig.setDescription(config.getDescription());
        existingConfig.setCommand(config.getCommand());
        existingConfig.setArgs(config.getArgs());
        existingConfig.setEnv(config.getEnv());
        existingConfig.setEndpoint(config.getEndpoint());
        existingConfig.setSseEndpoint(config.getSseEndpoint());
        existingConfig.setHeaders(config.getHeaders());
        existingConfig.setConnectTimeoutMs(config.getConnectTimeoutMs());
        existingConfig.setRequestTimeoutMs(config.getRequestTimeoutMs());
        existingConfig.setInitTimeoutMs(config.getInitTimeoutMs());
        existingConfig.setUpdatedAt(LocalDateTime.now());

        return mcpServerConfigRepository.save(existingConfig);
    }

    /**
     * 删除 MCP Server 配置
     *
     * 为什么：防止删除不存在的配置，保持操作语义清晰
     * 入参：ID 查询对象
     * 出参：无
     */
    @Override
    public void deleteMcpServerConfig(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("MCP Server ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        /*
         * 目的：先检查存在性，避免静默失败
         */
        if (!mcpServerConfigRepository.existsById(idQuery)) {
            throw new NotFoundException("MCP Server 配置不存在，id: " + id);
        }
        mcpServerConfigRepository.deleteById(idQuery);
    }

    /**
     * 启用 MCP Server
     *
     * 为什么：启用后可被运行时加载
     * 入参：ID 查询对象
     * 出参：启用后的配置
     */
    @Override
    public McpServerConfig enableMcpServer(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("MCP Server ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "MCP Server 配置不存在，id: " + id;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        McpServerConfig config = mcpServerConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        config.setEnabled(true);
        config.setUpdatedAt(LocalDateTime.now());
        return mcpServerConfigRepository.save(config);
    }

    /**
     * 禁用 MCP Server
     *
     * 为什么：禁用后避免运行时继续使用
     * 入参：ID 查询对象
     * 出参：禁用后的配置
     */
    @Override
    public McpServerConfig disableMcpServer(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("MCP Server ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "MCP Server 配置不存在，id: " + id;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        McpServerConfig config = mcpServerConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        config.setEnabled(false);
        config.setUpdatedAt(LocalDateTime.now());
        return mcpServerConfigRepository.save(config);
    }

    /**
     * 查询启用的 MCP Server
     *
     * 为什么：运行时只加载启用配置
     * 入参：启用状态查询对象
     * 出参：启用配置列表
     */
    @Override
    public List<McpServerConfig> queryEnabledServers(EnabledQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("启用状态查询条件不能为空");
        }
        return mcpServerConfigRepository.findByEnabled(query);
    }
}
