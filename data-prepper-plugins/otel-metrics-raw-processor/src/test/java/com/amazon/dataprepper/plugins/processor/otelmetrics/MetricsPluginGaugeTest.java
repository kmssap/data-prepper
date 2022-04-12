/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.dataprepper.plugins.processor.otelmetrics;

import com.amazon.dataprepper.model.configuration.PluginSetting;
import com.amazon.dataprepper.model.metric.Metric;
import com.amazon.dataprepper.model.record.Record;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Exemplar;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.amazon.dataprepper.plugins.processor.otelmetrics.OTelMetricsProtoHelperTest.getRandomBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@RunWith(MockitoJUnitRunner.class)
public class MetricsPluginGaugeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC);
    private OTelMetricsRawProcessor rawProcessor;

    @Before
    public void init() {
        PluginSetting testsettings = new PluginSetting("testsettings", Collections.emptyMap());
        testsettings.setPipelineName("testpipeline");
        rawProcessor = new OTelMetricsRawProcessor(testsettings);
    }

    @Test
    public void testInstrumentationLibrary() throws JsonProcessingException {
        NumberDataPoint.Builder p1 = NumberDataPoint.newBuilder().setAsInt(4);
        Gauge gauge = Gauge.newBuilder().addDataPoints(p1).build();

        io.opentelemetry.proto.metrics.v1.Metric.Builder metric = io.opentelemetry.proto.metrics.v1.Metric.newBuilder()
                .setGauge(gauge)
                .setUnit("seconds")
                .setName("name")
                .setDescription("description");

        InstrumentationLibraryMetrics isntLib = InstrumentationLibraryMetrics.newBuilder()
                .addMetrics(metric)
                .setInstrumentationLibrary(InstrumentationLibrary.newBuilder()
                        .setName("ilname")
                        .setVersion("ilversion")
                        .build())
                .build();

        Resource resource = Resource.newBuilder()
                .addAttributes(KeyValue.newBuilder()
                        .setKey("service.name")
                        .setValue(AnyValue.newBuilder().setStringValue("service").build())
                ).build();

        ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder()
                .addInstrumentationLibraryMetrics(isntLib)
                .setResource(resource)
                .build();

        ExportMetricsServiceRequest exportMetricRequest = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(resourceMetrics).build();

        Record<ExportMetricsServiceRequest> record = new Record<>(exportMetricRequest);

        Collection<Record<? extends Metric>> records = rawProcessor.doExecute(Collections.singletonList(record));
        List<Record<? extends Metric>> list = new ArrayList<>(records);

        Record<? extends Metric> dataPrepperResult = list.get(0);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(dataPrepperResult.getData().toJsonString(), Map.class);
        assertThat(map).contains(entry("kind", Metric.KIND.GAUGE.toString()));
        assertThat(map).contains(entry("unit", "seconds"));
        assertThat(map).contains(entry("serviceName", "service"));
        assertThat(map).contains(entry("resource.attributes.service@name", "service"));
        assertThat(map).contains(entry("description", "description"));
        assertThat(map).contains(entry("value", 4.0D));
        assertThat(map).contains(entry("startTime","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("time","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("time","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("instrumentationLibrary.name", "ilname"));
        assertThat(map).contains(entry("instrumentationLibrary.version", "ilversion"));

    }

    @Test
    public void testScopeMetricsLibrary() throws JsonProcessingException {
        NumberDataPoint.Builder p1 = NumberDataPoint.newBuilder().setAsInt(4);
        Gauge gauge = Gauge.newBuilder().addDataPoints(p1).build();

        io.opentelemetry.proto.metrics.v1.Metric.Builder metric = io.opentelemetry.proto.metrics.v1.Metric.newBuilder()
                .setGauge(gauge)
                .setUnit("seconds")
                .setName("name")
                .setDescription("description");

        ScopeMetrics scopeMetrics = ScopeMetrics.newBuilder()
                .addMetrics(metric)
                .setScope(InstrumentationScope.newBuilder()
                        .setName("smname")
                        .setVersion("smversion"))
                .build();

        Resource resource = Resource.newBuilder()
                .addAttributes(KeyValue.newBuilder()
                        .setKey("service.name")
                        .setValue(AnyValue.newBuilder().setStringValue("service").build())
                ).build();

        ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder()
                .addScopeMetrics(scopeMetrics)
                .setResource(resource)
                .build();

        ExportMetricsServiceRequest exportMetricRequest = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(resourceMetrics).build();

        Record<ExportMetricsServiceRequest> record = new Record<>(exportMetricRequest);

        Collection<Record<? extends Metric>> records = rawProcessor.doExecute(Collections.singletonList(record));
        List<Record<? extends Metric>> list = new ArrayList<>(records);

        Record<? extends Metric> dataPrepperResult = list.get(0);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(dataPrepperResult.getData().toJsonString(), Map.class);

        assertThat(map).contains(entry("kind", Metric.KIND.GAUGE.toString()));
        assertThat(map).contains(entry("unit", "seconds"));
        assertThat(map).contains(entry("serviceName", "service"));
        assertThat(map).contains(entry("resource.attributes.service@name", "service"));
        assertThat(map).contains(entry("description", "description"));
        assertThat(map).contains(entry("value", 4.0D));
        assertThat(map).contains(entry("startTime","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("time","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("time","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("instrumentationScope.name", "smname"));
        assertThat(map).contains(entry("instrumentationScope.version", "smversion"));

    }

    @Test
    public void testWithExemplar() throws JsonProcessingException {
        long t1 = Instant.now(CLOCK).getEpochSecond();
        byte[] spanId = getRandomBytes(8);
        byte[] traceId = getRandomBytes(8);

        Exemplar e1 = Exemplar.newBuilder()
                .addFilteredAttributes(KeyValue.newBuilder()
                        .setKey("key")
                        .setValue(AnyValue.newBuilder().setBoolValue(true).build()).build())
                .setAsDouble(3)
                .setSpanId(ByteString.copyFrom(spanId))
                .setTimeUnixNano(t1)
                .setTraceId(ByteString.copyFrom(traceId))
                .build();

        NumberDataPoint.Builder p1 = NumberDataPoint.newBuilder()
                .addExemplars(e1)
                .setAsInt(4)
                .setFlags(1);

        Gauge gauge = Gauge.newBuilder().addDataPoints(p1).build();

        io.opentelemetry.proto.metrics.v1.Metric.Builder metric = io.opentelemetry.proto.metrics.v1.Metric.newBuilder()
                .setGauge(gauge)
                .setUnit("seconds")
                .setName("name")
                .setDescription("description");

        ScopeMetrics scopeMetrics = ScopeMetrics.newBuilder()
                .addMetrics(metric)
                .setScope(InstrumentationScope.newBuilder()
                        .setName("smname")
                        .setVersion("smversion"))
                .build();

        Resource resource = Resource.newBuilder()
                .addAttributes(KeyValue.newBuilder()
                        .setKey("service.name")
                        .setValue(AnyValue.newBuilder().setStringValue("service").build())
                ).build();

        ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder()
                .addScopeMetrics(scopeMetrics)
                .setResource(resource)
                .build();

        ExportMetricsServiceRequest exportMetricRequest = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(resourceMetrics).build();

        Record<ExportMetricsServiceRequest> record = new Record<>(exportMetricRequest);

        Collection<Record<? extends Metric>> records = rawProcessor.doExecute(Collections.singletonList(record));
        List<Record<? extends Metric>> list = new ArrayList<>(records);

        Record<? extends Metric> dataPrepperResult = list.get(0);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(dataPrepperResult.getData().toJsonString(), Map.class);

        assertThat(map).contains(entry("kind", Metric.KIND.GAUGE.toString()));
        assertThat(map).contains(entry("unit", "seconds"));
        assertThat(map).contains(entry("serviceName", "service"));
        assertThat(map).contains(entry("resource.attributes.service@name", "service"));
        assertThat(map).contains(entry("description", "description"));
        assertThat(map).contains(entry("value", 4.0D));
        assertThat(map).contains(entry("startTime","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("time","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("instrumentationScope.name", "smname"));
        assertThat(map).contains(entry("instrumentationScope.version", "smversion"));
        assertThat(map).contains(entry("flags", 1));

        List<Map<String, Object>> exemplars = (List<Map<String, Object>>) map.get("exemplars");
        assertThat(exemplars.size()).isEqualTo(1);
        Map<String, Object> eTest = exemplars.get(0);

        assertThat(eTest).contains(entry("time", "2023-11-14T22:13:20Z"));
        assertThat(eTest).contains(entry("value", 3.0));
        assertThat(eTest).contains(entry("spanId", Hex.encodeHexString(spanId)));
        assertThat(eTest).contains(entry("traceId", Hex.encodeHexString(traceId)));
        Map<String, Object> atts = (Map<String, Object>)eTest.get("attributes");
        assertThat(atts).contains(entry("exemplar.attributes.key", true));
    }
}
