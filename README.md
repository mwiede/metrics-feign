# metrics-feign [![travis status](https://travis-ci.org/mwiede/metrics-feign.svg?branch=master)](https://travis-ci.org/mwiede/metrics-feign) [![Maven Central](https://img.shields.io/maven-central/v/com.github.mwiede/metrics-feign.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.mwiede%22%20AND%20a:%22metrics-feign%22)
A decorator wrapping [Feign](https://github.com/OpenFeign/feign) client method handlers in order to provide [Dropwizard Metrics](http://metrics.dropwizard.io) of calls to feign target interfaces.

## Usage with Feign 10

Feign offers [Metrics4Capability](https://github.com/OpenFeign/feign/blob/master/dropwizard-metrics4/src/main/java/feign/metrics4/Metrics4Capability.java), which gives basic metrics, but in order to use metric annotations, add `com.github.mwiede.metrics.feign.AnnotionMetricsCapability` like this:

```java

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

```

## Usage with Feign < 10.11

Basically you only have to replace ```Feign.builder()``` with ```FeignWithMetrics.builder(metricRegistry)```.

```java

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
```

## List of provided metrics

Based of the example above, the following metrics were registered and reported:

### Meters
* com.github.mwiede.metrics.example.Example$GitHub.contributors.1xx-responses
* com.github.mwiede.metrics.example.Example$GitHub.contributors.2xx-responses
* com.github.mwiede.metrics.example.Example$GitHub.contributors.3xx-responses
* com.github.mwiede.metrics.example.Example$GitHub.contributors.4xx-responses
* com.github.mwiede.metrics.example.Example$GitHub.contributors.5xx-responses
* com.github.mwiede.metrics.example.Example$GitHub.contributors.Metered
* com.github.mwiede.metrics.example.Example$GitHub.contributors.exceptions
* com.github.mwiede.metrics.example.Example$GitHub.contributors.reAttempts.Metered
* com.github.mwiede.metrics.example.Example$GitHub.contributors.retryExhausted.Metered
### Timers
com.github.mwiede.metrics.example.Example$GitHub.contributors.Timed

## Download

You can use this library via maven:

```xml
<dependency>
  <groupId>com.github.mwiede</groupId>
  <artifactId>metrics-feign</artifactId>
  <version>3.1</version>
</dependency>
```



