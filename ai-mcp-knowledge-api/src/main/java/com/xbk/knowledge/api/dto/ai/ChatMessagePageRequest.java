package com.xbk.knowledge.api.dto.ai;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话消息分页查询请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMessagePageRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    private Long sessionId;
}
