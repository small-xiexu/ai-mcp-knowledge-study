package com.xbk.knowledge.application.support.xxl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author xiexu
 */
public class XxlJobIdParserTest {

    @Test
    public void shouldParseDigitsOnly() {
        assertEquals(123L, XxlJobIdParser.parseJobIdOrNull("123"));
        assertEquals(123L, XxlJobIdParser.parseJobIdOrNull("  123 "));
    }

    @Test
    public void shouldRejectNonDigits() {
        assertNull(XxlJobIdParser.parseJobIdOrNull(null));
        assertNull(XxlJobIdParser.parseJobIdOrNull(""));
        assertNull(XxlJobIdParser.parseJobIdOrNull("ok"));
        assertNull(XxlJobIdParser.parseJobIdOrNull("{\"code\":200,\"content\":\"123\"}"));
        assertNull(XxlJobIdParser.parseJobIdOrNull("jobId=123"));
    }
}

