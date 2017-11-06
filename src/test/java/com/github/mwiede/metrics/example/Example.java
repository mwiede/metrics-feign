package com.github.mwiede.metrics.example;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.github.mwiede.metrics.feign.FeignOutboundMetricsDecorator;

import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;

/**
 * An example showing how {@link FeignOutboundMetricsDecorator} can be used.
 */
public class Example {

    @Timed
    @Metered
    @ExceptionMetered
    interface GitHub {
        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);
    }

    static class Contributor {
        String login;
        int contributions;
    }

    public static void main(String... args) {

        MetricRegistry metricRegistry = new MetricRegistry();

        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();

        GitHub github = Feign.builder().invocationHandlerFactory(
                new FeignOutboundMetricsDecorator(new InvocationHandlerFactory.Default(), metricRegistry))
                .decoder(new GsonDecoder()).target(GitHub.class, "https://api.github.com");

        // Fetch and print a list of the contributors to this library.
        List<Contributor> contributors = github.contributors("mwiede", "metrics-feign");
        for (Contributor contributor : contributors) {
            System.out.println(contributor.login + " (" + contributor.contributions + ")");
        }

        reporter.report();
    }
}
