/**
 * 对话记忆基础设施适配层。
 *
 * 职责：封装 Spring AI ChatMemoryRepository 在 Redis 的落地实现，
 * 统一会话记忆的读写、序列化与过期策略。
 *
 * @author sxie
 */
package com.xbk.knowledge.infrastructure.chatmemory;
