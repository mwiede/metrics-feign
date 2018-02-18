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
import org.junit.Ignore;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import feign.Retryer;

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

  @Test
  public void testNoRetryer() {
    stubFor(post(anyUrl()).willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
    final MyClientWithoutAnnotation target =
        FeignWithMetrics
            .builder(metricRegistry)
            .retryer(Retryer.NEVER_RETRY)
            .target(MyClientWithoutAnnotation.class,
                String.format("http://localhost:%d", wireMockRule.port()));

    try {
      target.myMethod();
    } catch (final Exception e) {
    } finally {
      assertEquals(2, metricRegistry.getMeters().size());
      assertEquals(
          0,
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

  @Test
  @Ignore
  public void test100() {
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(100)));
    final MyClientWithAnnotationOnMethodLevel target =
        FeignWithMetrics.builder(metricRegistry).target(MyClientWithAnnotationOnMethodLevel.class,
            String.format("http://localhost:%d", wireMockRule.port()));


    target.myMethod();

    assertResponseMeterMetric(100);
  }

  @Test
  public void test200() {
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(201)));
    final MyClientWithAnnotationOnMethodLevel target =
        FeignWithMetrics.builder(metricRegistry).target(MyClientWithAnnotationOnMethodLevel.class,
            String.format("http://localhost:%d", wireMockRule.port()));


    target.myMethod();

    assertResponseMeterMetric(201);
  }

  @Test
  public void test300() {
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(302)));
    final MyClientWithAnnotationOnMethodLevel target =
        FeignWithMetrics.builder(metricRegistry).target(MyClientWithAnnotationOnMethodLevel.class,
            String.format("http://localhost:%d", wireMockRule.port()));


    try {
      target.myMethod();
    } catch (final Exception e) {
    } finally {
      assertResponseMeterMetric(302);
    }
  }

  @Test
  public void test400() {
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(400)));
    final MyClientWithAnnotationOnMethodLevel target =
        FeignWithMetrics.builder(metricRegistry).target(MyClientWithAnnotationOnMethodLevel.class,
            String.format("http://localhost:%d", wireMockRule.port()));


    try {
      target.myMethod();
    } catch (final Exception e) {
    } finally {
      assertResponseMeterMetric(400);
    }
  }

  @Test
  public void test500() {
    stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500)));
    final MyClientWithAnnotationOnMethodLevel target =
        FeignWithMetrics.builder(metricRegistry).target(MyClientWithAnnotationOnMethodLevel.class,
            String.format("http://localhost:%d", wireMockRule.port()));


    try {
      target.myMethod();
    } catch (final Exception e) {
    } finally {
      assertResponseMeterMetric(500);
    }
  }

  private void assertResponseMeterMetric(final int code) {
    assertEquals(
        code <= 200 ? 1 : 0,
        metricRegistry
            .getMeters()
            .get(
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.1xx-responses")
            .getCount());
    assertEquals(
        code >= 200 && code < 300 ? 1 : 0,
        metricRegistry
            .getMeters()
            .get(
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.2xx-responses")
            .getCount());
    assertEquals(
        code >= 300 && code < 400 ? 1 : 0,
        metricRegistry
            .getMeters()
            .get(
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.3xx-responses")
            .getCount());
    assertEquals(
        code >= 400 && code < 500 ? 1 : 0,
        metricRegistry
            .getMeters()
            .get(
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.4xx-responses")
            .getCount());
    assertEquals(
        code >= 500 ? 1 : 0,
        metricRegistry
            .getMeters()
            .get(
                "com.github.mwiede.metrics.feign.MyClientWithAnnotationOnMethodLevel.myMethod.5xx-responses")
            .getCount());
  }

}
