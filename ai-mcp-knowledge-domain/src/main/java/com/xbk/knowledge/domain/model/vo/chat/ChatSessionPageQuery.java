package com.xbk.knowledge.domain.model.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话分页查询条件值对象
 *
 * 职责：领域值对象，用于表达分页查询条件
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionPageQuery {

    /**
     * 偏移量
     */
    private Integer offset;

    /**
     * 每页大小
     */
    private Integer pageSize;
}
