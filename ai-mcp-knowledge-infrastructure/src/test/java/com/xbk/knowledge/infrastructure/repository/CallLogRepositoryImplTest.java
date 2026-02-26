package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.metrics.model.aggregate.CallLogAggregate;
import com.xbk.knowledge.domain.metrics.model.entity.CallLog;
import com.xbk.knowledge.domain.llm.model.valobj.ModelIdQuery;
import com.xbk.knowledge.infrastructure.dao.ICallLogDao;
import com.xbk.knowledge.infrastructure.dao.po.CallLogPO;
import com.xbk.knowledge.infrastructure.repository.metrics.CallLogRepositoryImpl;
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
        ICallLogDao mapper = Mockito.mock(ICallLogDao.class);
        CallLogRepositoryImpl repository = new CallLogRepositoryImpl(mapper);

        CallLog callLog = CallLog.builder().modelId(1L).build();
        CallLogAggregate aggregate = CallLogAggregate.builder().callLog(callLog).build();

        CallLogAggregate saved = repository.save(aggregate);

        assertNotNull(saved.getCallLog().getCreatedAt());
        Mockito.verify(mapper).insertCallLog(any(CallLogPO.class));
    }

    /**
     * 对外暴露 shouldReturnEmptyWhenModelIdMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyWhenModelIdMissing() {
        ICallLogDao mapper = Mockito.mock(ICallLogDao.class);
        CallLogRepositoryImpl repository = new CallLogRepositoryImpl(mapper);

        assertTrue(repository.findByModelId(new ModelIdQuery(null)).isEmpty());
    }
}
