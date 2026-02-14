package com.xbk.knowledge.application.support.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRagGovernanceSupportTest {

    @Mock
    private RagVectorStoreService ragVectorStoreService;

    @Test
    void requiredModeWithoutAnyTags_shouldMarkRequiredMiss() {
        AgentRagGovernanceSupport support = new AgentRagGovernanceSupport(new ObjectMapper(), ragVectorStoreService);
        AgentVersion v = AgentVersion.builder()
                .ragMode("REQUIRED")
                .defaultRagTagsJson(null)
                .allowedRagTagsJson(null)
                .build();
        AgentRagGovernanceSupport.ResolvedRag rag = support.resolve(v, null, "hello");
        assertNotNull(rag);
        assertTrue(rag.required());
        assertTrue(rag.requiredMiss());
        assertTrue(rag.documents().isEmpty());
    }

    @Test
    void disabledMode_shouldNotSearchAndNotMiss() {
        AgentRagGovernanceSupport support = new AgentRagGovernanceSupport(new ObjectMapper(), ragVectorStoreService);
        AgentVersion v = AgentVersion.builder()
                .ragMode("DISABLED")
                .build();
        AgentRagGovernanceSupport.ResolvedRag rag = support.resolve(v, "[\"a\"]", "hello");
        assertNotNull(rag);
        assertFalse(rag.required());
        assertFalse(rag.requiredMiss());
        assertTrue(rag.documents().isEmpty());
    }

    @Test
    void allowedTags_shouldDropOutOfListAndSearchWithEffectiveOnHit() {
        AgentRagGovernanceSupport s = new AgentRagGovernanceSupport(new ObjectMapper(), ragVectorStoreService);

        when(ragVectorStoreService.similaritySearch(anyString(), anyList()))
                .thenReturn(List.of(new Document("doc-1", Map.of("knowledge", "tagA"))));

        AgentVersion v = AgentVersion.builder()
                .ragMode("OPTIONAL")
                .allowedRagTagsJson("[\"tagA\"]")
                .build();

        AgentRagGovernanceSupport.ResolvedRag rag = s.resolve(v, "[\"tagA\",\"tagB\"]", "q");
        assertNotNull(rag);
        assertEquals(List.of("tagA"), rag.effectiveTags());
        assertEquals(List.of("tagB"), rag.droppedTags());
        assertFalse(rag.requiredMiss());
        assertEquals(1, rag.documents().size());
        assertEquals(1, rag.citations().size());
        assertTrue(rag.citations().get(0).getSource().startsWith("rag"));
    }
}
