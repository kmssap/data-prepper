/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.dataprepper.plugins.processor.otelmetrics;

import com.amazon.dataprepper.model.annotations.DataPrepperPlugin;
import com.amazon.dataprepper.model.configuration.PluginSetting;
import com.amazon.dataprepper.model.metric.ExponentialHistogram;
import com.amazon.dataprepper.model.metric.Gauge;
import com.amazon.dataprepper.model.metric.Histogram;
import com.amazon.dataprepper.model.metric.JacksonExponentialHistogram;
import com.amazon.dataprepper.model.metric.JacksonGauge;
import com.amazon.dataprepper.model.metric.JacksonHistogram;
import com.amazon.dataprepper.model.metric.JacksonSum;
import com.amazon.dataprepper.model.metric.JacksonSummary;
import com.amazon.dataprepper.model.metric.Metric;
import com.amazon.dataprepper.model.metric.Sum;
import com.amazon.dataprepper.model.metric.Summary;
import com.amazon.dataprepper.model.processor.AbstractProcessor;
import com.amazon.dataprepper.model.processor.Processor;
import com.amazon.dataprepper.model.record.Record;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;

import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@DataPrepperPlugin(name = "otel_metrics_raw_processor", pluginType = Processor.class)
public class OTelMetricsRawProcessor extends AbstractProcessor<Record<ExportMetricsServiceRequest>, Record<? extends Metric>> {

    public OTelMetricsRawProcessor(PluginSetting pluginSetting) {
        super(pluginSetting);
    }

    @Override
    public Collection<Record<? extends Metric>> doExecute(Collection<Record<ExportMetricsServiceRequest>> records) {
        Collection<Record<? extends Metric>> recordsOut = new ArrayList<>();

        for (Record<ExportMetricsServiceRequest> ets : records) {
            for (ResourceMetrics rs : ets.getData().getResourceMetricsList()) {
                final String schemaUrl = rs.getSchemaUrl();
                final Map<String, Object> resourceAttributes = OTelMetricsProtoHelper.getResourceAttributes(rs.getResource());
                final String serviceName = OTelMetricsProtoHelper.getServiceName(rs.getResource()).orElse(null);

                for (InstrumentationLibraryMetrics is : rs.getInstrumentationLibraryMetricsList()) {
                    final Map<String, Object> ils = OTelMetricsProtoHelper.getInstrumentationLibraryAttributes(is.getInstrumentationLibrary());
                    recordsOut.addAll(processMetricsList(is.getMetricsList(), serviceName, ils, resourceAttributes, schemaUrl));
                }

                for (ScopeMetrics sm : rs.getScopeMetricsList()) {
                    final Map<String, Object> ils = OTelMetricsProtoHelper.getInstrumentationScopeAttributes(sm.getScope());
                    recordsOut.addAll(processMetricsList(sm.getMetricsList(), serviceName, ils, resourceAttributes, schemaUrl));
                }

            }
        }
        return recordsOut;
    }

    private List<? extends Record<? extends Metric>> processMetricsList(final List<io.opentelemetry.proto.metrics.v1.Metric> metricsList,
                                                                        final String serviceName,
                                                                        final Map<String, Object> ils,
                                                                        final Map<String, Object> resourceAttributes,
                                                                        final String schemaUrl) {
        List<Record<? extends Metric>> recordsOut = new ArrayList<>();
        for (io.opentelemetry.proto.metrics.v1.Metric metric : metricsList) {
            if (metric.hasGauge()) {
                recordsOut.addAll(mapGauge(metric, serviceName, ils, resourceAttributes, schemaUrl));
            } else if (metric.hasSum()) {
                recordsOut.addAll(mapSum(metric, serviceName, ils, resourceAttributes, schemaUrl));
            } else if (metric.hasSummary()) {
                recordsOut.addAll(mapSummary(metric, serviceName, ils, resourceAttributes, schemaUrl));
            } else if (metric.hasHistogram()) {
                recordsOut.addAll(mapHistogram(metric, serviceName, ils, resourceAttributes, schemaUrl));
            } else if (metric.hasExponentialHistogram()) {
                recordsOut.addAll(mapExponentialHistogram(metric, serviceName, ils, resourceAttributes, schemaUrl));
            }
        }
        return recordsOut;
    }

    private List<Record<Gauge>> mapGauge(io.opentelemetry.proto.metrics.v1.Metric metric,
                                         String serviceName,
                                         final Map<String, Object> ils,
                                         final Map<String, Object> resourceAttributes,
                                         final String schemaUrl) {
        return metric.getGauge().getDataPointsList().stream()
                .map(dp -> (Gauge) JacksonGauge.builder()
                        .withUnit(metric.getUnit())
                        .withName(metric.getName())
                        .withDescription(metric.getDescription())
                        .withStartTime(OTelMetricsProtoHelper.getStartTimeISO8601(dp))
                        .withTime(OTelMetricsProtoHelper.getTimeISO8601(dp))
                        .withServiceName(serviceName)
                        .withValue(OTelMetricsProtoHelper.getValueAsDouble(dp))
                        .withAttributes(OTelMetricsProtoHelper.mergeAllAttributes(
                                Arrays.asList(
                                        OTelMetricsProtoHelper.convertKeysOfDataPointAttributes(dp),
                                        resourceAttributes,
                                        ils
                                )
                        ))
                        .withSchemaUrl(schemaUrl)
                        .withExemplars(OTelMetricsProtoHelper.convertExemplars(dp.getExemplarsList()))
                        .withFlags(dp.getFlags())
                        .build())
                .map(Record::new)
                .collect(Collectors.toList());
    }

    private List<Record<Sum>> mapSum(final io.opentelemetry.proto.metrics.v1.Metric metric,
                                     final String serviceName,
                                     final Map<String, Object> ils,
                                     final Map<String, Object> resourceAttributes,
                                     final String schemaUrl) {
        return metric.getSum().getDataPointsList().stream()
                .map(dp -> (Sum) JacksonSum.builder()
                        .withUnit(metric.getUnit())
                        .withName(metric.getName())
                        .withDescription(metric.getDescription())
                        .withStartTime(OTelMetricsProtoHelper.getStartTimeISO8601(dp))
                        .withTime(OTelMetricsProtoHelper.getTimeISO8601(dp))
                        .withServiceName(serviceName)
                        .withIsMonotonic(metric.getSum().getIsMonotonic())
                        .withValue(OTelMetricsProtoHelper.getValueAsDouble(dp))
                        .withAggregationTemporality(metric.getSum().getAggregationTemporality().toString())
                        .withAttributes(OTelMetricsProtoHelper.mergeAllAttributes(
                                Arrays.asList(
                                        OTelMetricsProtoHelper.convertKeysOfDataPointAttributes(dp),
                                        resourceAttributes,
                                        ils
                                )
                        ))
                        .withSchemaUrl(schemaUrl)
                        .withExemplars(OTelMetricsProtoHelper.convertExemplars(dp.getExemplarsList()))
                        .withFlags(dp.getFlags())
                        .build())
                .map(Record::new)
                .collect(Collectors.toList());
    }

    private List<Record<Summary>> mapSummary(final io.opentelemetry.proto.metrics.v1.Metric metric,
                                             final String serviceName,
                                             final Map<String, Object> ils,
                                             final Map<String, Object> resourceAttributes,
                                             final String schemaUrl) {
        return metric.getSummary().getDataPointsList().stream()
                .map(dp -> (Summary) JacksonSummary.builder()
                        .withUnit(metric.getUnit())
                        .withName(metric.getName())
                        .withDescription(metric.getDescription())
                        .withStartTime(OTelMetricsProtoHelper.convertUnixNanosToISO8601(dp.getStartTimeUnixNano()))
                        .withTime(OTelMetricsProtoHelper.convertUnixNanosToISO8601(dp.getTimeUnixNano()))
                        .withServiceName(serviceName)
                        .withCount(dp.getCount())
                        .withSum(dp.getSum())
                        .withQuantiles(OTelMetricsProtoHelper.getQuantileValues(dp.getQuantileValuesList()))
                        .withQuantilesValueCount(dp.getQuantileValuesCount())
                        .withAttributes(OTelMetricsProtoHelper.mergeAllAttributes(
                                Arrays.asList(
                                        OTelMetricsProtoHelper.unpackKeyValueList(dp.getAttributesList()),
                                        resourceAttributes,
                                        ils
                                )
                        ))
                        .withSchemaUrl(schemaUrl)
                        .withFlags(dp.getFlags())
                        .build())
                .map(Record::new)
                .collect(Collectors.toList());
    }

    private List<Record<Histogram>> mapHistogram(final io.opentelemetry.proto.metrics.v1.Metric metric,
                                                 final String serviceName,
                                                 final Map<String, Object> ils,
                                                 final Map<String, Object> resourceAttributes,
                                                 final String schemaUrl) {
        return metric.getHistogram().getDataPointsList().stream()
                .map(dp -> (Histogram) JacksonHistogram.builder()
                        .withUnit(metric.getUnit())
                        .withName(metric.getName())
                        .withDescription(metric.getDescription())
                        .withStartTime(OTelMetricsProtoHelper.convertUnixNanosToISO8601(dp.getStartTimeUnixNano()))
                        .withTime(OTelMetricsProtoHelper.convertUnixNanosToISO8601(dp.getTimeUnixNano()))
                        .withServiceName(serviceName)
                        .withSum(dp.getSum())
                        .withCount(dp.getCount())
                        .withBucketCount(dp.getBucketCountsCount())
                        .withExplicitBoundsCount(dp.getExplicitBoundsCount())
                        .withAggregationTemporality(metric.getHistogram().getAggregationTemporality().toString())
                        .withBuckets(OTelMetricsProtoHelper.createBuckets(dp.getBucketCountsList(), dp.getExplicitBoundsList()))
                        .withAttributes(OTelMetricsProtoHelper.mergeAllAttributes(
                                Arrays.asList(
                                        OTelMetricsProtoHelper.unpackKeyValueList(dp.getAttributesList()),
                                        resourceAttributes,
                                        ils
                                )
                        ))
                        .withSchemaUrl(schemaUrl)
                        .withExemplars(OTelMetricsProtoHelper.convertExemplars(dp.getExemplarsList()))
                        .withFlags(dp.getFlags())
                        .build())
                .map(Record::new)
                .collect(Collectors.toList());
    }

    private List<? extends Record<? extends Metric>> mapExponentialHistogram(io.opentelemetry.proto.metrics.v1.Metric metric, String serviceName, Map<String, Object> ils, Map<String, Object> resourceAttributes, String schemaUrl) {
        return metric.getExponentialHistogram().getDataPointsList().stream()
                .map(dp -> (ExponentialHistogram) JacksonExponentialHistogram.builder()
                        .withUnit(metric.getUnit())
                        .withName(metric.getName())
                        .withDescription(metric.getDescription())
                        .withStartTime(OTelMetricsProtoHelper.convertUnixNanosToISO8601(dp.getStartTimeUnixNano()))
                        .withTime(OTelMetricsProtoHelper.convertUnixNanosToISO8601(dp.getTimeUnixNano()))
                        .withServiceName(serviceName)
                        .withSum(dp.getSum())
                        .withCount(dp.getCount())
                        .withZeroCount(dp.getZeroCount())
                        .withScale(dp.getScale())
                        .withPositiveBuckets(OTelMetricsProtoHelper.createExponentialBuckets(dp.getPositive(), dp.getScale()))
                        .withNegativeBuckets(OTelMetricsProtoHelper.createExponentialBuckets(dp.getNegative(), dp.getScale()))
                        .withAggregationTemporality(metric.getHistogram().getAggregationTemporality().toString())
                        .withAttributes(OTelMetricsProtoHelper.mergeAllAttributes(
                                Arrays.asList(
                                        OTelMetricsProtoHelper.unpackKeyValueList(dp.getAttributesList()),
                                        resourceAttributes,
                                        ils
                                )
                        ))
                        .withSchemaUrl(schemaUrl)
                        .withExemplars(OTelMetricsProtoHelper.convertExemplars(dp.getExemplarsList()))
                        .withFlags(dp.getFlags())
                        .build())
                .map(Record::new)
                .collect(Collectors.toList());

    }


    @Override
    public void prepareForShutdown() {
    }

    @Override
    public boolean isReadyForShutdown() {
        return true;
    }

    @Override
    public void shutdown() {
    }
}
