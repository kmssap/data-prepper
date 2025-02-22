/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.processor.otelmetrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.Sum;
import io.opentelemetry.proto.resource.v1.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensearch.dataprepper.model.configuration.PluginSetting;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.record.Record;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@RunWith(MockitoJUnitRunner.class)
public class MetricsPluginSumTest {

    OTelMetricsRawProcessor rawProcessor;

    @Before
    public void init() {
        PluginSetting testsettings = new PluginSetting("testsettings", Collections.emptyMap());
        testsettings.setPipelineName("testpipeline");
        rawProcessor = new OTelMetricsRawProcessor(testsettings, new OtelMetricsRawProcessorConfig());
    }

    @Test
    public void test() throws JsonProcessingException {

        final KeyValue childAttr1 = KeyValue.newBuilder().setKey("statement").setValue(AnyValue.newBuilder()
                .setIntValue(1_000).build()).build();
        final KeyValue childAttr2 = KeyValue.newBuilder().setKey("statement.params").setValue(AnyValue.newBuilder()
                .setStringValue("us-east-1").build()).build();

        final KeyValue attribute1 = KeyValue.newBuilder().setKey("db.details").setValue(AnyValue.newBuilder()
                .setKvlistValue(KeyValueList.newBuilder().addAllValues(Arrays.asList(childAttr1, childAttr2)).build()).build()).build();
        final KeyValue attribute2 = KeyValue.newBuilder().setKey("http.status").setValue(AnyValue.newBuilder()
                .setStringValue("4xx").build()).build();

        final AnyValue anyValue1 = AnyValue.newBuilder().setStringValue("asdf").build();
        final AnyValue anyValue2 = AnyValue.newBuilder().setDoubleValue(2000.123).build();
        final AnyValue anyValue3 = AnyValue.newBuilder().setKvlistValue(KeyValueList.newBuilder().addAllValues(Arrays.asList(childAttr1, childAttr2))).build();
        final ArrayValue arrayValue = ArrayValue.newBuilder().addAllValues(Arrays.asList(anyValue1, anyValue2, anyValue3)).build();
        final KeyValue attribute3 = KeyValue.newBuilder().setKey("aws.details").setValue(AnyValue.newBuilder()
                .setArrayValue(arrayValue)).build();


        NumberDataPoint dataPoint = NumberDataPoint.newBuilder()
                .setAsInt(3)
                .addAllAttributes(Arrays.asList(attribute1, attribute2, attribute3))
                .setFlags(0)
                .build();
        Sum sum = Sum.newBuilder().addAllDataPoints(Collections.singletonList(dataPoint)).build();

        Metric metric = Metric.newBuilder()
                .setSum(sum)
                .setUnit("seconds")
                .setName("name")
                .setDescription("description")
                .build();

        InstrumentationLibraryMetrics instLib = InstrumentationLibraryMetrics.newBuilder()
                .setInstrumentationLibrary(InstrumentationLibrary.newBuilder().setVersion("v1").setName("name").build())
                .addMetrics(metric).build();

        Resource resource = Resource.newBuilder()
                .addAttributes(KeyValue.newBuilder()
                        .setKey("service.name")
                        .setValue(AnyValue.newBuilder().setStringValue("service").build())
                ).build();

        ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder()
                .setResource(resource)
                .addInstrumentationLibraryMetrics(instLib)
                .build();

        ExportMetricsServiceRequest exportMetricRequest = ExportMetricsServiceRequest.newBuilder().addResourceMetrics(resourceMetrics).build();

        Record record = new Record<>(exportMetricRequest);

        List<Record<Event>> rec = (List<Record<Event>>) rawProcessor.doExecute(Arrays.asList(record));
        Record<Event> firstRecord = rec.get(0);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(firstRecord.getData().toJsonString(), Map.class);
        assertSumProcessing(map);
    }

    private void assertSumProcessing(Map<String, Object> map) {
        assertThat(map).contains(entry("kind", org.opensearch.dataprepper.model.metric.Metric.KIND.SUM.toString()));
        assertThat(map).contains(entry("unit", "seconds"));
        assertThat(map).contains(entry("description", "description"));
        assertThat(map).contains(entry("name", "name"));
        assertThat(map).contains(entry("serviceName", "service"));
        assertThat(map).contains(entry("value", 3D));
        assertThat(map).contains(entry("metric.attributes.http@status", "4xx"));
        assertThat(map).contains(entry("isMonotonic", false));
        assertThat(map).contains(entry("aggregationTemporality","AGGREGATION_TEMPORALITY_UNSPECIFIED"));
        assertThat(map).contains(entry("metric.attributes.db@details", "{\"statement@params\":\"us-east-1\",\"statement\":1000}"));
        assertThat(map).contains(entry("startTime","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("time","1970-01-01T00:00:00Z"));
        assertThat(map).contains(entry("instrumentationLibrary.version", "v1"));
        assertThat(map).contains(entry("instrumentationLibrary.name", "name"));
        assertThat(map).contains(entry("metric.attributes.aws@details", "[\"asdf\",2000.123,\"{\\\"statement@params\\\":\\\"us-east-1\\\",\\\"statement\\\":1000}\"]"));
        assertThat(map).contains(entry("flags", 0));

    }

}
