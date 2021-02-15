package com.github.mwiede.metrics.example;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.ResponseMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.mwiede.metrics.feign.AnnotionMetricsCapability;
import com.github.mwiede.metrics.feign.FeignMetricsInvocationHandlerFactoryDecorator;
import feign.Feign;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An example showing how {@link FeignMetricsInvocationHandlerFactoryDecorator} can be used.
 */
public class Example4 {

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

    final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate("feign");

    final ConsoleReporter reporter =
        ConsoleReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS).build();

    final GitHub github =
        Feign.builder().decoder(new GsonDecoder())
                .addCapability(new AnnotionMetricsCapability(metricRegistry))
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
