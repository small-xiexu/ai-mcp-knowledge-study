package com.xbk.knowledge.types.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SSE 事件封装（delta/final 等）。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStreamEvent implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 事件名称。
     */
    private String name;

    /**
     * 事件负载。
     */
    private Object data;
}
