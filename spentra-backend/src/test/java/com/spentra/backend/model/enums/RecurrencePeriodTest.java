package com.spentra.backend.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class RecurrencePeriodTest {

    @Test
    void testEnumConstants() {
        assertNotNull(RecurrencePeriod.valueOf("NONE"));
        assertNotNull(RecurrencePeriod.valueOf("DAILY"));
        assertNotNull(RecurrencePeriod.valueOf("WEEKLY"));
        assertNotNull(RecurrencePeriod.valueOf("MONTHLY"));
        assertNotNull(RecurrencePeriod.valueOf("YEARLY"));

        assertEquals(5, RecurrencePeriod.values().length);
    }
}
