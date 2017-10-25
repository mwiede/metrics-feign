package com.github.mwiede.metrics.feign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;

import feign.InvocationHandlerFactory;
import feign.Target;

/**
 * Testing whether {@link FeignOutboundMetricsDecorator} registers the correct metrics in {@link MetricRegistry}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FeignOutboundMetricsDecoratorTest {

    @Mock
    InvocationHandlerFactory invocationHandlerFactory;

    @Mock
    Target target;

    @Mock
    InvocationHandlerFactory.MethodHandler methodHandler;

    Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

    MetricRegistry metricRegistry;

    @InjectMocks
    FeignOutboundMetricsDecorator feignOutboundMetricsDecorator;

    @Before
    public void init() {
        metricRegistry = new MetricRegistry();
        feignOutboundMetricsDecorator = new FeignOutboundMetricsDecorator(invocationHandlerFactory, metricRegistry);
    }

    @Test
    public void createEmpty() throws Exception {
        dispatch = Collections.emptyMap();
        feignOutboundMetricsDecorator.create(target, dispatch);
        assertTrue("metricRegistry should not contain any metric.", metricRegistry.getNames().isEmpty());
    }

    @Test
    public void createOnMethodLevelWithoutAnnotation() throws Exception {
        dispatch = new HashMap<>();
        dispatch.put(MyClientWithoutAnnotation.class.getMethods()[0], methodHandler);
        feignOutboundMetricsDecorator.create(target, dispatch);
        assertTrue("metricRegistry should not contain any metric.", metricRegistry.getNames().isEmpty());
    }

    @Test
    public void createOnMethodLevelWithAnnotation() throws Exception {
        dispatch = new HashMap<>();
        dispatch.put(MyClientWithAnnotationOnMethodLevel.class.getMethods()[0], methodHandler);
        feignOutboundMetricsDecorator.create(target, dispatch);
        assertEquals("metricRegistry should contain 3 metrics.", 3, metricRegistry.getNames().size());
    }

    @Test
    public void createOnClassAndMethodLevelWithAnnotation() throws Exception {
        dispatch = new HashMap<>();
        dispatch.put(MyClientWithAnnotationOnClassAndMethodLevel.class.getMethods()[0], methodHandler);
        feignOutboundMetricsDecorator.create(target, dispatch);
        assertEquals("metricRegistry should contain 3 metrics.", 3, metricRegistry.getNames().size());
    }

    @Test
    public void createOnClassLevelWithAnnotation() throws Exception {
        dispatch = new HashMap<>();
        dispatch.put(MyClientWithAnnotationOnClassLevel.class.getMethods()[0], methodHandler);
        feignOutboundMetricsDecorator.create(target, dispatch);
        assertEquals("metricRegistry should contain 3 metrics.", 3, metricRegistry.getNames().size());
    }

    @After
    public void after() {
        verify(invocationHandlerFactory).create(target, dispatch);
    }

}