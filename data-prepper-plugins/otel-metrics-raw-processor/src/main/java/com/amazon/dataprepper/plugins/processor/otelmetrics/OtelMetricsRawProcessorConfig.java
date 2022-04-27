/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.dataprepper.plugins.processor.otelmetrics;

public class OtelMetricsRawProcessorConfig {

    private Boolean calculateHistogramBuckets = false;

    private Boolean calculateExponentialHistogramBuckets = false;

    public Boolean getCalculateExponentialHistogramBuckets() {
        return calculateExponentialHistogramBuckets;
    }

    public Boolean getCalculateHistogramBuckets() {
        return calculateHistogramBuckets;
    }
}
