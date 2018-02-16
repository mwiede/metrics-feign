package com.github.mwiede.metrics.feign;

import java.io.IOException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import com.github.mwiede.metrics.feign.FeignOutboundMetricsDecorator.ResponseMeterMetric;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;

public class MetricCollectingClient extends Client.Default {

  public MetricCollectingClient(final SSLSocketFactory sslContextFactory,
      final HostnameVerifier hostnameVerifier) {
    super(sslContextFactory, hostnameVerifier);
  }

  @Override
  public Response execute(final Request request, final Options options) throws IOException {

    final ResponseMeterMetric metric = FeignOutboundMetricsDecorator.ACTUAL_METRIC.get();

    final Response response = super.execute(request, options);

    if (metric != null && response != null) {
      final int responseStatus = response.status() / 100;
      if (responseStatus >= 1 && responseStatus <= 5) {
        metric.meters.get(responseStatus - 1).mark();
      }
    }

    return response;
  }

}
