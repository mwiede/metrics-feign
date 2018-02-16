package com.github.mwiede.metrics.feign;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import feign.Feign;
import feign.FeignException;
import feign.InvocationHandlerFactory;

/**
 * Testing, whether the {@link FeignOutboundMetricsDecorator.MethodHandler} really triggers the
 * registered metrics.
 */
@RunWith(MockitoJUnitRunner.class)
public class FeignOutboundMetricsMethodHandlerTest {

  @ClassRule
  public static WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
      .dynamicPort());

  MetricRegistry metricRegistry;

  @BeforeClass
  public static void initWiremock() {
    // Catch-all case
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
  }

  @Before
  public void init() {
    metricRegistry = new MetricRegistry();
  }

  @Test
  public void nothingIsMetered() {
    final MyClientWithoutAnnotation target =
        Feign
            .builder()
            .invocationHandlerFactory(
                new FeignOutboundMetricsDecorator(new InvocationHandlerFactory.Default(),
                    metricRegistry))
            .target(MyClientWithoutAnnotation.class,
                String.format("http://localhost:%d", wireMockRule.port()));

    target.myMethod();

    assertTrue("Expected to have no metrics registered and tracked.", metricRegistry.getMetrics()
        .isEmpty());

  }

  @Test
  public void timedPerClassMethodsAreTimed() {
    final MyClientWithAnnotationOnClassLevel target =
        Feign
            .builder()
            .invocationHandlerFactory(
                new FeignOutboundMetricsDecorator(new InvocationHandlerFactory.Default(),
                    metricRegistry))
            .target(MyClientWithAnnotationOnClassLevel.class,
                String.format("http://localhost:%d", wireMockRule.port()));

    target.myMethod();

    assertMetrics();

  }

  @Test
  public void timedPerMethodsAreTimed() {
    final MyClientWithAnnotationOnMethodLevel target =
        Feign
            .builder()
            .invocationHandlerFactory(
                new FeignOutboundMetricsDecorator(new InvocationHandlerFactory.Default(),
                    metricRegistry))
            .target(MyClientWithAnnotationOnMethodLevel.class,
                String.format("http://localhost:%d", wireMockRule.port()));

    target.myMethod();

    assertMetrics();
  }

  @Test(expected = FeignException.class)
  public void exception() {
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(400)));
    final MyClientWithAnnotationOnMethodLevel target =
        Feign
            .builder()
            .invocationHandlerFactory(
                new FeignOutboundMetricsDecorator(new InvocationHandlerFactory.Default(),
                    metricRegistry))
            .target(MyClientWithAnnotationOnMethodLevel.class,
                String.format("http://localhost:%d", wireMockRule.port()));
    try {
      target.myMethod();
    } finally {

      assertMetrics();

      final Set<Map.Entry<String, Meter>> entries = metricRegistry.getMeters().entrySet();

      entries.forEach(entry -> {
        if (entry.getKey().endsWith(ExceptionMetered.DEFAULT_NAME_SUFFIX)) {
          assertEquals(String.format("wrong number of invocations in metric %s", entry.getKey()),
              1, entry.getValue().getCount());
        }
      });
    }
  }

  private void assertMetrics() {
    final Timer timer = metricRegistry.getTimers().values().iterator().next();
    assertEquals("wrong number of invocations in metric.", 1, timer.getCount());

    assertTrue("wrong value of mean in metric.", timer.getMeanRate() > 0);

    assertEquals("wrong number of meter metrics.", 7, metricRegistry.getMeters().values().size());

    final Set<Map.Entry<String, Meter>> entries = metricRegistry.getMeters().entrySet();

    entries.forEach(entry -> {
      if (entry.getKey().endsWith("Metered")) {
        assertEquals(String.format("wrong number of invocations in metric %s", entry.getKey()), 1,
            entry.getValue().getCount());
      }
    });

  }
}
