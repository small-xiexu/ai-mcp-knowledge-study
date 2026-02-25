package com.xbk.knowledge.api;

/**
 * MCP 网关接入服务接口
 * 定义 MCP 协议接入端点的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IMcpGatewayService {

    /**
     * 建立 SSE 长连接。
     * 
     * @param gatewayId 网关 ID
     * @param apiKey Header 传入的 API Key
     * @param apiKeyQuery Query 传入的 API Key
     * @return SSE 事件流
     */
    Object establishSseConnection(String gatewayId, String apiKey, String apiKeyQuery);

    /**
     * 处理 MCP 消息请求。
     * 
     * @param gatewayId 网关 ID
     * @param sessionId 会话 ID
     * @param body 请求报文
     * @return 消息响应结果
     */
    Object handleMessage(String gatewayId, String sessionId, String body);
}
