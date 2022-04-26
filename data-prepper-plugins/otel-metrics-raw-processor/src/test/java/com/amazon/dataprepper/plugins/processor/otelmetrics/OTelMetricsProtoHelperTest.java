/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.dataprepper.plugins.processor.otelmetrics;

import com.amazon.dataprepper.model.metric.Bucket;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Exemplar;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import org.apache.commons.codec.binary.Hex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test exists purely to satisfy the test coverage because OtelMetricsHelper must be merged with
 * OtelProtoCodec when #546 is integrated since it shares most of the code with OTelProtoCodec
 */
public class OTelMetricsProtoHelperTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC);

    private static final Random RANDOM = new Random();

    public static byte[] getRandomBytes(int len) {
        byte[] bytes = new byte[len];
        RANDOM.nextBytes(bytes);
        return bytes;
    }


    @Test
    void getValueAsDouble() {
        assertNull(OTelMetricsProtoHelper.getValueAsDouble(NumberDataPoint.newBuilder().build()));
    }

    @Test
    public void testCreateBucketsEmpty() {
        assertThat(OTelMetricsProtoHelper.createBuckets(new ArrayList<>(), new ArrayList<>()).size(), equalTo(0));
    }

    @Test
    public void testCreateBuckets() {
        List<Long> bucketsCountList = Arrays.asList(1L, 2L, 3L, 4L);
        List<Double> explicitBOundsList = Arrays.asList(5D, 10D, 25D);
        List<Bucket> buckets = OTelMetricsProtoHelper.createBuckets(bucketsCountList, explicitBOundsList);
        assertThat(buckets.size(), equalTo(4));
        Bucket b1 = buckets.get(0);
        assertThat(b1.getCount(), equalTo(1L));
        assertThat(b1.getMin(), equalTo((double) -Float.MAX_VALUE));
        assertThat(b1.getMax(), equalTo(5D));

        Bucket b2 = buckets.get(1);
        assertThat(b2.getCount(), equalTo(2L));
        assertThat(b2.getMin(), equalTo(5D));
        assertThat(b2.getMax(), equalTo(10D));

        Bucket b3 = buckets.get(2);
        assertThat(b3.getCount(), equalTo(3L));
        assertThat(b3.getMin(), equalTo(10D));
        assertThat(b3.getMax(), equalTo(25D));

        Bucket b4 = buckets.get(3);
        assertThat(b4.getCount(), equalTo(4L));
        assertThat(b4.getMin(), equalTo(25D));
        assertThat(b4.getMax(), equalTo((double) Float.MAX_VALUE));
    }

    @Test
    public void testCreateBuckets_illegal_argument() {
        List<Long> bucketsCountList = Arrays.asList(1L, 2L, 3L, 4L);
        List<Double> boundsList = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> OTelMetricsProtoHelper.createBuckets(bucketsCountList, boundsList));
    }


    @Test
    public void testConvertAnyValueBool() {
        Object o = OTelMetricsProtoHelper.convertAnyValue(AnyValue.newBuilder().setBoolValue(true).build());
        assertThat(o instanceof Boolean, equalTo(true));
        assertThat(((boolean) o), equalTo(true));
    }

    @Test
    public void testUnsupportedTypeToAnyValue() {
        assertThrows(RuntimeException.class,
                () -> OTelMetricsProtoHelper.convertAnyValue(AnyValue.newBuilder().setBytesValue(ByteString.EMPTY).build()));
    }

    @Test
    void convertExemplars() {
        long t1 = Instant.now(CLOCK).getEpochSecond();
        long t2 = t1 + 100_000;

        Exemplar e1 = Exemplar.newBuilder()
                .addFilteredAttributes(KeyValue.newBuilder()
                        .setKey("key")
                        .setValue(AnyValue.newBuilder().setBoolValue(true).build()).build())
                .setAsDouble(3)
                .setSpanId(ByteString.copyFrom(getRandomBytes(8)))
                .setTimeUnixNano(t1)
                .setTraceId(ByteString.copyFrom(getRandomBytes(8)))
                .build();


        Exemplar e2 = Exemplar.newBuilder()
                .addFilteredAttributes(KeyValue.newBuilder()
                        .setKey("key2")
                        .setValue(AnyValue.newBuilder()
                                .setArrayValue(ArrayValue.newBuilder().addValues(AnyValue.newBuilder().setStringValue("test").build()).build())
                                .build()).build())
                .setAsInt(42)
                .setSpanId(ByteString.copyFrom(getRandomBytes(8)))
                .setTimeUnixNano(t2)
                .setTraceId(ByteString.copyFrom(getRandomBytes(8)))
                .build();

        List<io.opentelemetry.proto.metrics.v1.Exemplar> exemplars = Arrays.asList(e1, e2);
        List<com.amazon.dataprepper.model.metric.Exemplar> convertedExemplars = OTelMetricsProtoHelper.convertExemplars(exemplars);
        assertThat(convertedExemplars.size(), equalTo(2));

        com.amazon.dataprepper.model.metric.Exemplar conv1 = convertedExemplars.get(0);
        assertThat(conv1.getSpanId(), equalTo(Hex.encodeHexString(e1.getSpanId().toByteArray())));
        assertThat(conv1.getTime(), equalTo("2023-11-14T22:13:20Z"));
        assertThat(conv1.getTraceId(), equalTo(Hex.encodeHexString(e1.getTraceId().toByteArray())));
        assertThat(conv1.getValue(), equalTo(3.0));
        Assertions.assertThat(conv1.getAttributes()).contains(entry("exemplar.attributes.key", true));

        com.amazon.dataprepper.model.metric.Exemplar conv2 = convertedExemplars.get(1);
        assertThat(conv2.getSpanId(), equalTo(Hex.encodeHexString(e2.getSpanId().toByteArray())));
        assertThat(conv2.getTime(), equalTo("2023-11-16T02:00:00Z"));
        assertThat(conv2.getTraceId(), equalTo(Hex.encodeHexString(e2.getTraceId().toByteArray())));
        assertThat(conv2.getValue(), equalTo(42.0));
        Assertions.assertThat(conv2.getAttributes()).contains(entry("exemplar.attributes.key2", "[\"test\"]"));

    }
}