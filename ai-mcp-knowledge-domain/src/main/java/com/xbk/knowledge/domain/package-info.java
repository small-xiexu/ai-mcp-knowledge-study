/**
 * 领域层（Domain Layer）。
 *
 * 职责：定义业务核心模型、仓储端口与领域服务，不依赖具体基础设施实现。
 *
 * 结构约定：
 * - model：领域对象（聚合、实体、值对象）
 * - repository：仓储抽象（端口）
 * - service：领域服务与领域规则编排
 *
 * @author xiexu
 */
package com.xbk.knowledge.domain;
