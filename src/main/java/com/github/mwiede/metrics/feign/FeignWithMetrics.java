package com.github.mwiede.metrics.feign;

import com.codahale.metrics.MetricRegistry;

import feign.Client;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Retryer;

/**
 * Extension of {@link feign.Feign} which includes a builder which decorates some of the default
 * implementations. {@link InvocationHandlerFactory.Default} is decorated with
 * {@link FeignMetricsInvocationHandlerFactoryDecorator}, {@link Retryer.Default} with {@link FeignMetricsRetryerDecorator} and
 * {@link Client.Default} with {@link FeignMetricsClientDecorator}.
 */
public abstract class FeignWithMetrics {

  /**
   * Convenience method to instantiate a {@link feign.Feign.Builder} including the classes to
   * configure necessary classes to collect metrics.
   * 
   * @param metricRegistry
   * @return the builder
   */
  public static feign.Feign.Builder builder(final MetricRegistry metricRegistry) {
    return new FeignWithMetrics.Builder(metricRegistry)
        .invocationHandlerFactory(new InvocationHandlerFactory.Default())
        .retryer(new Retryer.Default()).client(new Client.Default(null, null));

  }

  public static class Builder extends Feign.Builder {

    private final MetricRegistry metricRegistry;

    public Builder(final MetricRegistry metricRegistry) {
      super();
      this.metricRegistry = metricRegistry;
    }

    @Override
    public feign.Feign.Builder client(final Client client) {
      return super.client(new FeignMetricsClientDecorator(client));
    }

    @Override
    public feign.Feign.Builder invocationHandlerFactory(
        final InvocationHandlerFactory invocationHandlerFactory) {
      return super.invocationHandlerFactory(new FeignMetricsInvocationHandlerFactoryDecorator(
          invocationHandlerFactory, metricRegistry));
    }

    @Override
    public feign.Feign.Builder retryer(final Retryer retryer) {
      return super.retryer(new FeignMetricsRetryerDecorator(retryer, metricRegistry));
    }
  }

}
