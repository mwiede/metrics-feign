package com.github.mwiede.metrics.feign;

import com.codahale.metrics.MetricRegistry;

import feign.Client;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Retryer;

/**
 * Extension of {@link feign.Feign} which includes a builder which uses
 * {@link FeignOutboundMetricsDecorator} instead of {@link InvocationHandlerFactory.Default},
 * {@link MetricCollectingRetryer} instead of {@link Retryer.Default} and
 * {@link MetricCollectingClient} instead of {@link Client.Default}.
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
    return new Feign.Builder()
        .invocationHandlerFactory(
            new FeignOutboundMetricsDecorator(new InvocationHandlerFactory.Default(),
                metricRegistry)).retryer(new MetricCollectingRetryer(metricRegistry))
        .client(new MetricCollectingClient(null, null));

  }

}
