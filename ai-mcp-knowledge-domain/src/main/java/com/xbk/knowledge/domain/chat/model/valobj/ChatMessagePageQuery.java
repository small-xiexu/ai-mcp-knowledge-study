package com.xbk.knowledge.domain.chat.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息分页查询条件值对象
 *
 * 职责：领域值对象，用于表达分页查询条件
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePageQuery {

    /**
     * 会话ID
     *
     * 为什么：限定消息所属会话
     */
    private Long sessionId;

    /**
     * 偏移量
     *
     * 为什么：用于分页计算起始位置
     */
    private Integer offset;

    /**
     * 每页大小
     *
     * 为什么：控制单次返回数量
     */
    private Integer pageSize;
}
