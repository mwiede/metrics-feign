package com.github.mwiede.metrics.feign;

import com.codahale.metrics.MetricRegistry;
import feign.Capability;
import feign.Client;
import feign.InvocationHandlerFactory;
import feign.Retryer;

/**
 * A {@link Capability} to enrich {@link feign.Feign} with dropwizard annotion-based metrics.
 */
public class AnnotionMetricsCapability implements Capability {

    private final MetricRegistry metricRegistry;

    public AnnotionMetricsCapability(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public Client enrich(Client client) {
        return new FeignMetricsClientDecorator(client);
    }

    @Override
    public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory) {
        return new FeignMetricsInvocationHandlerFactoryDecorator(invocationHandlerFactory, metricRegistry);
    }

    @Override
    public Retryer enrich(Retryer retryer) {
        return new FeignMetricsRetryerDecorator(retryer, metricRegistry);
    }
}
