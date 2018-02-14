package com.github.mwiede.metrics.feign;

import com.codahale.metrics.MetricRegistry;

import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Retryer;

/**
 * Extension of {@link feign.Feign} which includes a builder which uses
 * {@link FeignOutboundMetricsDecorator} instead of {@link InvocationHandlerFactory.Default} and
 * {@link MetricExposingRetryer} instead of {@link Retryer.Default}.
 */
public abstract class FeignWithMetrics {

  /**
   * Convenience method to instantiate a {@link feign.Feign.Builder} including the classes to
   * configure necessary classes to collect metrics.
   * 
   * @param metricRegistry
   * @return the builder
   */
  public static Feign.Builder builder(final MetricRegistry metricRegistry) {
    return new Feign.Builder().invocationHandlerFactory(
        new FeignOutboundMetricsDecorator(new InvocationHandlerFactory.Default(), metricRegistry))
        .retryer(new MetricExposingRetryer(metricRegistry));

  }

}
