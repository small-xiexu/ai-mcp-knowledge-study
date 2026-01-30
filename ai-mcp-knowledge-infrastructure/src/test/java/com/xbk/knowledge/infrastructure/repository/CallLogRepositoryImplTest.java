package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.aggregate.call.CallLogAggregate;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.vo.model.ModelIdQuery;
import com.xbk.knowledge.infrastructure.mapper.CallLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * 验证调用日志仓储的时间戳补齐与空参保护。
 *
 * @author xiexu
 */
public class CallLogRepositoryImplTest {

    /**
     * 对外暴露 shouldSetCreatedAtOnSave 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSetCreatedAtOnSave() {
        CallLogMapper mapper = Mockito.mock(CallLogMapper.class);
        CallLogRepositoryImpl repository = new CallLogRepositoryImpl(mapper);

        CallLog callLog = CallLog.builder().modelId(1L).build();
        CallLogAggregate aggregate = CallLogAggregate.builder().callLog(callLog).build();

        CallLogAggregate saved = repository.save(aggregate);

        assertNotNull(saved.getCallLog().getCreatedAt());
        Mockito.verify(mapper).insertCallLog(any(CallLog.class));
    }

    /**
     * 对外暴露 shouldReturnEmptyWhenModelIdMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyWhenModelIdMissing() {
        CallLogMapper mapper = Mockito.mock(CallLogMapper.class);
        CallLogRepositoryImpl repository = new CallLogRepositoryImpl(mapper);

        assertTrue(repository.findByModelId(new ModelIdQuery(null)).isEmpty());
    }
}
