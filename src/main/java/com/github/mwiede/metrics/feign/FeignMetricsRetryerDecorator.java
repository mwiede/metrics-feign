package com.github.mwiede.metrics.feign;

import java.lang.reflect.Method;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import feign.RetryableException;
import feign.Retryer;

/**
 * A {@link Retryer} which exposes a metric which shows the number of attempts being made during
 * invocation on the target. It can only be used together with {@link FeignMetricsInvocationHandlerFactoryDecorator}
 * because it takes the actual invoked method from its threadlocal.
 *
 */
public class FeignMetricsRetryerDecorator implements Retryer {

  private final MetricRegistry metricRegistry;
  private final Retryer delegate;

  public FeignMetricsRetryerDecorator(final Retryer retryer, final MetricRegistry metricRegistry) {
    this.delegate = retryer;
    this.metricRegistry = metricRegistry;
  }

  @Override
  public void continueOrPropagate(final RetryableException e) {
    final Meter meter = getMetric("reAttempts");

    try {
      delegate.continueOrPropagate(e);
      meter.mark();
    } catch (final Exception ex) {
      getMetric("retryExhausted").mark();
      throw ex;
    }
  }

  @Override
  public Retryer clone() {
    return new FeignMetricsRetryerDecorator(delegate.clone(), metricRegistry);
  }

  private Meter getMetric(final String metricName) {
    final Method method = FeignMetricsInvocationHandlerFactoryDecorator.ACTUAL_METHOD.get();
    final String name =
        FeignMetricsInvocationHandlerFactoryDecorator.chooseName("", false, method, metricName, "Metered");
    return metricRegistry.meter(name);
  }


}
