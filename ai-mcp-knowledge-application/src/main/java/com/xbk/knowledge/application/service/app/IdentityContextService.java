package com.xbk.knowledge.application.service.app;

/**
 * 身份上下文服务。
 *
 * 职责：应用层抽象，用于收口登录态读取与话操作，隔离上层对 Sa-Token 的直接依赖。
 *
 * @author sxie
 */
public interface IdentityContextService {

    /**
     * 建立登录态。
     * 
     * @param userId 用户ID
     */
    void login(Long userId);

    /**
     * 退出登录态。
     */
    void logout();

    /**
     * 获取当前登录用户ID。
     * 
     * @return 用户ID
     */
    Long getCurrentUserId();

    /**
     * 判断当前是否已登录。
     * 
     * @return 是否已登录
     */
    boolean isLogin();

    /**
     * 获取当前 token 名称。
     * 
     * @return token 名称
     */
    String getTokenName();

    /**
     * 获取当前 token 值。
     * 
     * @return token 值
     */
    String getTokenValue();

    /**
     * 获取 token 剩余有效时长（秒）。
     * 
     * @return 剩余秒数
     */
    long getTokenTimeout();
}
