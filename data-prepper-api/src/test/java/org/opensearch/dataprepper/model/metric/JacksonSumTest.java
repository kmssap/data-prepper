/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.model.metric;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JacksonSumTest {

    private static final Map<String, Object> TEST_ATTRIBUTES = ImmutableMap.of(
            "key1", new Date().getTime(),
            "key2", UUID.randomUUID().toString());
    private static final String TEST_SERVICE_NAME = "service";
    private static final String TEST_NAME = "name";
    private static final String TEST_DESCRIPTION = "description";
    private static final String TEST_UNIT_NAME = "unit";
    private static final String TEST_START_TIME = UUID.randomUUID().toString();
    private static final String TEST_TIME = UUID.randomUUID().toString();
    private static final String TEST_AGGREGATION_TEMPORALITY = "TESTTEMPORALITY";
    private static final String TEST_EVENT_KIND = Metric.KIND.SUM.name();
    private static final boolean TEST_IS_MONOTONIC = true;
    private static final Double TEST_VALUE = 1D;
    private static final String TEST_SCHEMA_URL = "schema";

    private JacksonSum sum;

    private JacksonSum.Builder builder;

    @BeforeEach
    public void setup() {
        builder = JacksonSum.builder()
                .withAttributes(TEST_ATTRIBUTES)
                .withName(TEST_NAME)
                .withDescription(TEST_DESCRIPTION)
                .withEventKind(TEST_EVENT_KIND)
                .withAggregationTemporality(TEST_AGGREGATION_TEMPORALITY)
                .withStartTime(TEST_START_TIME)
                .withTime(TEST_TIME)
                .withUnit(TEST_UNIT_NAME)
                .withIsMonotonic(TEST_IS_MONOTONIC)
                .withValue(TEST_VALUE)
                .withServiceName(TEST_SERVICE_NAME)
                .withSchemaUrl(TEST_SCHEMA_URL);

        sum = builder.build();

    }

    @Test
    public void testGetAttributes() {
        final Map<String, Object> attributes = sum.getAttributes();
        TEST_ATTRIBUTES.keySet().forEach(key -> {
                    assertThat(attributes, hasKey(key));
                    assertThat(attributes.get(key), is(equalTo(TEST_ATTRIBUTES.get(key))));
                }
        );
    }

    @Test
    public void testGetName() {
        final String name = sum.getName();
        assertThat(name, is(equalTo(TEST_NAME)));
    }

    @Test
    public void testGetDescription() {
        final String description = sum.getDescription();
        assertThat(description, is(equalTo(TEST_DESCRIPTION)));
    }


    @Test
    public void testGetKind() {
        final String kind = sum.getKind();
        assertThat(kind, is(equalTo(TEST_EVENT_KIND)));
    }

    @Test
    public void testGetServiceName() {
        final String name = sum.getServiceName();
        assertThat(name, is(equalTo(TEST_SERVICE_NAME)));
    }

    @Test
    public void testGetAggregationTemporality() {
        final String aggregationTemporality = sum.getAggregationTemporality();
        assertThat(aggregationTemporality, is(equalTo(TEST_AGGREGATION_TEMPORALITY)));
    }


    @Test
    public void testGetStartTime() {
        final String GetStartTime = sum.getStartTime();
        assertThat(GetStartTime, is(equalTo(TEST_START_TIME)));
    }

    @Test
    public void testGetTime() {
        final String endTime = sum.getTime();
        assertThat(endTime, is(equalTo(TEST_TIME)));
    }

    @Test
    public void testGetUnit() {
        final String unit = sum.getUnit();
        assertThat(unit, is(equalTo(TEST_UNIT_NAME)));
    }

    @Test
    public void testGetMonotonic() {
        final boolean monotonic = sum.isMonotonic();
        assertThat(monotonic, is(equalTo(TEST_IS_MONOTONIC)));
    }

    @Test
    public void testGetValue() {
        final Double value = sum.getValue();
        assertThat(value, is(equalTo(TEST_VALUE)));
    }

    @Test
    public void testBuilder_missingNonNullParameters_throwsNullPointerException() {
        final JacksonSum.Builder builder = JacksonSum.builder();
        builder.withValue(null);
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    public void testBuilder_withEmptyTime_throwsIllegalArgumentException() {
        builder.withTime("");
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void testGetSchemaUrl() {
        final String url = sum.getSchemaUrl();
        assertThat(url, is(equalTo(TEST_SCHEMA_URL)));
    }

}