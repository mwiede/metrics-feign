package com.github.mwiede.metrics.feign;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class FeignWithMetricsTest {

  @ClassRule
  public static WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
      .dynamicPort());

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {}

  MetricRegistry metricRegistry;

  @Before
  public void setUp() throws Exception {
    metricRegistry = new MetricRegistry();
  }

  @Test
  public void test404() {
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(404)));
    final MyClientWithoutAnnotation target =
        FeignWithMetrics.builder(metricRegistry).target(MyClientWithoutAnnotation.class,
            String.format("http://localhost:%d", wireMockRule.port()));

    try {
      target.myMethod();
    } catch (final Exception e) {
    } finally {
      assertTrue("Expected to have no metrics registered and tracked.", metricRegistry.getMetrics()
          .isEmpty());
    }
  }

  @Test
  public void testIOException() {
    stubFor(post(anyUrl()).willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
    final MyClientWithoutAnnotation target =
        FeignWithMetrics.builder(metricRegistry).target(MyClientWithoutAnnotation.class,
            String.format("http://localhost:%d", wireMockRule.port()));

    try {
      target.myMethod();
    } catch (final Exception e) {
    } finally {
      assertEquals(2, metricRegistry.getMeters().size());
      assertEquals(
          4,
          metricRegistry
              .getMeters()
              .get(
                  "com.github.mwiede.metrics.feign.MyClientWithoutAnnotation.myMethod.reAttempts.Metered")
              .getCount());
      assertEquals(
          1,
          metricRegistry
              .getMeters()
              .get(
                  "com.github.mwiede.metrics.feign.MyClientWithoutAnnotation.myMethod.retryExhausted.Metered")
              .getCount());
    }
  }

}
