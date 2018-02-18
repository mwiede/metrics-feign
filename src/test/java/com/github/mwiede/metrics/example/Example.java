package com.github.mwiede.metrics.example;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.ResponseMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.mwiede.metrics.feign.FeignMetricsInvocationHandlerFactoryDecorator;
import com.github.mwiede.metrics.feign.FeignWithMetrics;

import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;

/**
 * An example showing how {@link FeignMetricsInvocationHandlerFactoryDecorator} can be used.
 */
public class Example {

  @Timed
  @Metered
  @ExceptionMetered
  @ResponseMetered
  interface GitHub {
    @RequestLine("GET /repos/{owner}/{repo}/contributors")
    List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);
  }

  static class Contributor {
    String login;
    int contributions;
  }

  public static void main(final String... args) {

    final MetricRegistry metricRegistry = new MetricRegistry();

    final ConsoleReporter reporter =
        ConsoleReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS).build();

    final GitHub github =
        FeignWithMetrics.builder(metricRegistry).decoder(new GsonDecoder())
            .target(GitHub.class, "https://api.github.com");
    try {
      // Fetch and print a list of the contributors to this library.
      final List<Contributor> contributors = github.contributors("mwiede", "metrics-feign");
      for (final Contributor contributor : contributors) {
        System.out.println(contributor.login + " (" + contributor.contributions + ")");
      }
    } finally {
      reporter.report();
    }
  }
}
