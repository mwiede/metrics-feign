package com.github.mwiede.metrics.feign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * Testing whether {@link FeignMetricsInvocationHandlerFactoryDecorator} registers the correct metrics in
 * {@link MetricRegistry}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FeignMetricsInvocationHandlerFactoryDecoratorTest {

  @Mock
  InvocationHandlerFactory invocationHandlerFactory;

  @Mock
  Target target;

  @Mock
  InvocationHandlerFactory.MethodHandler methodHandler;

  Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

  MetricRegistry metricRegistry;

  @InjectMocks
  FeignMetricsInvocationHandlerFactoryDecorator feignMetricsInvocationHandlerFactoryDecorator;

  @Before
  public void init() {
    metricRegistry = new MetricRegistry();
    feignMetricsInvocationHandlerFactoryDecorator =
        new FeignMetricsInvocationHandlerFactoryDecorator(invocationHandlerFactory, metricRegistry);
  }

  @Test
  public void createEmpty() throws Exception {
    dispatch = Collections.emptyMap();
    feignMetricsInvocationHandlerFactoryDecorator.create(target, dispatch);
    assertTrue("metricRegistry should not contain any metric.", metricRegistry.getNames().isEmpty());
  }

  @Test
  public void createOnMethodLevelWithoutAnnotation() throws Exception {
    dispatch = new HashMap<>();
    dispatch.put(MyClientWithoutAnnotation.class.getMethods()[0], methodHandler);
    feignMetricsInvocationHandlerFactoryDecorator.create(target, dispatch);
    assertTrue("metricRegistry should not contain any metric.", metricRegistry.getNames().isEmpty());
  }

  @Test
  public void createOnMethodLevelWithAnnotation() throws Exception {
    dispatch = new HashMap<>();
    dispatch.put(MyClientWithAnnotationOnMethodLevel.class.getMethods()[0], methodHandler);
    feignMetricsInvocationHandlerFactoryDecorator.create(target, dispatch);
    assertEquals("metricRegistry should contain 8 metrics.", 8, metricRegistry.getNames().size());
    assertEquals(
        Stream
            .of("com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.1xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.2xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.3xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.4xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.5xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.Metered",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.Timed",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.exceptions")
            .sorted().collect(Collectors.toList()), Stream.of(metricRegistry.getNames().toArray())
            .sorted().collect(Collectors.toList()));
  }

  @Test
  public void createOnClassAndMethodLevelWithAnnotation() throws Exception {
    dispatch = new HashMap<>();
    dispatch.put(MyClientWithAnnotationOnClassAndMethodLevel.class.getMethods()[0], methodHandler);
    feignMetricsInvocationHandlerFactoryDecorator.create(target, dispatch);
    assertEquals("metricRegistry should contain 8 metrics.", 8, metricRegistry.getNames().size());
    assertEquals(
        Stream
            .of("com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.1xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.2xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.3xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.4xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.5xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.Metered",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.Timed",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassAndMethodLevel.myMethod.exceptions")
            .sorted().collect(Collectors.toList()), Stream.of(metricRegistry.getNames().toArray())
            .sorted().collect(Collectors.toList()));
  }

  @Test
  public void createOnClassLevelWithAnnotation() throws Exception {
    dispatch = new HashMap<>();
    dispatch.put(MyClientWithAnnotationOnClassLevel.class.getMethods()[0], methodHandler);
    feignMetricsInvocationHandlerFactoryDecorator.create(target, dispatch);
    assertEquals("metricRegistry should contain 8 metrics.", 8, metricRegistry.getNames().size());
    assertEquals(
        Stream
            .of("com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.1xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.2xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.3xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.4xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.5xx-responses",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.Metered",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.Timed",
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnClassLevel.myMethod.exceptions")
            .sorted().collect(Collectors.toList()), Stream.of(metricRegistry.getNames().toArray())
            .sorted().collect(Collectors.toList()));
  }

  @After
  public void after() {
    verify(invocationHandlerFactory).create(target, dispatch);
  }

}
